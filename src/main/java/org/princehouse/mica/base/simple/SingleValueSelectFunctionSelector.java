package org.princehouse.mica.base.simple;

import java.lang.reflect.Method;

import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.util.Distribution;

public class SingleValueSelectFunctionSelector extends Selector {
    private Method selectMethod;

    public SingleValueSelectFunctionSelector(Method selectMethod) {
        validateSelectMethod(selectMethod);
        this.selectMethod = selectMethod;
    }

    private static void validateSelectMethod(Method selectMethod) {
        // TODO: throw exception if method invalid
    }

    @Override
    public Distribution<Address> select(Protocol pinstance) {
        try {
            Object robj = selectMethod.invoke(pinstance);
            Address addr = (Address) robj;
            return Distribution.create(addr);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public String toString() {
        return String.format("<%s method %s>", getClass().getName(), selectMethod.getName());
    }
}