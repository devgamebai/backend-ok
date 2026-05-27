package bitzero.engine.io;

import bitzero.engine.io.filter.IFilterChain;

public interface IFilterSupport {
     IFilterChain getPreFilterChain();

     IFilterChain getPostFilterChain();
}
