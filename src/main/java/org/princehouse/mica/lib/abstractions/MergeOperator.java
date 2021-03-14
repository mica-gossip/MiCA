package org.princehouse.mica.lib.abstractions;

import java.util.List;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.util.Functional;

public abstract class MergeOperator {

    public abstract Merge merge(BaseProtocol p1, BaseProtocol p2);

    public BaseProtocol merge(List<BaseProtocol> plist) {
        // build a balanced tree of merges
        int n = plist.size();
        if (n == 1) {
            return plist.get(0);
        } else if (n == 2) {
            return merge(plist.get(0), plist.get(1));
        } else if (n % 2 == 0) {
            List<BaseProtocol> left = Functional.<BaseProtocol> sublist(plist, 0, n / 2);
            List<BaseProtocol> right = Functional.<BaseProtocol> sublist(plist, n / 2, n);
            return merge(merge(left), merge(right));
        } else { // n is odd, n >= 3
            List<BaseProtocol> left = Functional.<BaseProtocol> sublist(plist, 0, n / 2);
            List<BaseProtocol> right = Functional.<BaseProtocol> sublist(plist, n / 2, n); // odd
                                                                                           // length
            // return merge(merge(left), merge(right), ((double)left.size()) /
            // ((double)plist.size())); // balance the shares
            return merge(merge(left), merge(right)); // balance the shares
        }
    }
}
