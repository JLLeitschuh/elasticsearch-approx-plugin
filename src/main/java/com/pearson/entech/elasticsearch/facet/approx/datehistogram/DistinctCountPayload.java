package com.pearson.entech.elasticsearch.facet.approx.datehistogram;

import java.io.IOException;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.trove.map.TLongObjectMap;

import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.CountThenEstimate;
import com.clearspring.analytics.stream.cardinality.HyperLogLog;

public class DistinctCountPayload {

    private long _count;

    private CountThenEstimate _cardinality;

    public DistinctCountPayload(final int entryLimit) {
        _count = 0;
        // Enforce a sensible entry limit 
        //        final int realEntryLimit = entryLimit == 0 ? 1 : entryLimit;
        //        _cardinality = new CountThenEstimate(realEntryLimit,
        //                AdaptiveCounting.Builder.obyCount(realEntryLimit * 1000));
        _cardinality = new CountThenEstimate(entryLimit,
                new HyperLogLog.Builder(0.0025));
    }

    DistinctCountPayload(final long count, final CountThenEstimate cardinality) {
        _count = count;
        _cardinality = cardinality;
    }

    DistinctCountPayload(final long count, final byte[] cardinality) throws IOException, ClassNotFoundException {
        _count = count;
        _cardinality = new CountThenEstimate(cardinality);
    }

    DistinctCountPayload update(final Object item) {
        _count++;
        _cardinality.offer(item);
        return this;
    }

    byte[] cardinalityBytes() throws IOException {
        return _cardinality.getBytes();
    }

    long getCount() {
        return _count;
    }

    CountThenEstimate getCardinality() {
        return _cardinality;
    }

    DistinctCountPayload merge(final DistinctCountPayload other) throws CardinalityMergeException {
        _count += other._count;
        //        final int size = other._cardinality.sizeof();
        //        if(size == -1)
        //            System.out.println("Merging set with " + other._cardinality.cardinality() + " elements");
        //        else
        //            System.out.println("Merging estimator with size " + other._cardinality.sizeof() + " bytes");
        _cardinality = CountThenEstimate.mergeEstimators(this._cardinality, other._cardinality);
        return this;
    }

    DistinctCountPayload mergeInto(final TLongObjectMap<DistinctCountPayload> map, final long key) {
        if(map.containsKey(key))
            try {
                map.put(key, this.merge(map.get(key)));
            } catch(final CardinalityMergeException e) {
                throw new ElasticSearchException("Unable to merge two facet cardinality objects", e);
            }
        else
            map.put(key, this);
        return this;
    }
}