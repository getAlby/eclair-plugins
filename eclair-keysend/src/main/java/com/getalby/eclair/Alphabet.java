package com.getalby.eclair;

import scodec.bits.Bases;

public class Alphabet implements Bases.HexAlphabet {

    @Override
    public char toChar(int index) {
        return "0123456789abcdef".charAt(index);
    }

    @Override
    public int toIndex(char c) {
        return "0123456789abcdef".indexOf(c);
    }

    @Override
    public boolean ignore(char c) {
        return Character.isWhitespace(c);
    }
}
