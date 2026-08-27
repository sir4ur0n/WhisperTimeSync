package com.cubAIx.WhisperTimeSync;

import java.util.Vector;

public class TokenizedSent {
    public String text;
    public Vector<Token> tokens = new Vector<>();

    public TokenizedSent(String aTxt) {
        text = aTxt;
    }
}
