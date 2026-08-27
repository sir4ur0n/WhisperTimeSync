package com.cubAIx.WhisperTimeSync;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WhisperTimeSync {
    static final boolean _DEBUG_INOUT = false;
    static final boolean _DEBUG_ALIGN = false;

    public WhisperTimeSync() {
    }

    public static void main(String[] args) {
        try {
            new WhisperTimeSync().processFile(args[0], args[1], args[2]);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    String load(String aPath) throws Exception {
        StringBuilder aSB = new StringBuilder();
        BufferedReader aBR = new BufferedReader(
                new InputStreamReader(new FileInputStream(aPath)
                        , StandardCharsets.UTF_8));
        String aLine;
        while ((aLine = aBR.readLine()) != null) {
            if (!aSB.isEmpty()) {
                aSB.append("\n");
            }
            aSB.append(aLine);
        }
        aBR.close();
        return aSB.toString();
    }

    String toXml(String aSrt) {
        return ("\n" + aSrt.replaceAll("\r*\n", "\n"))
                .replaceAll("<", "&lt;").replaceAll(">", "&gt;")
                .replaceAll("\n([0-9]+)\n([0-9]+:[0-9]+:[0-9]+[,.][0-9]+ --&gt; [0-9]+:[0-9]+:[0-9]+[,.][0-9]+)\n"
                        , "<time id='$1' stamp='$2'/>")
                .replaceAll("\n([0-9]+:[0-9]+:[0-9]+[,.][0-9]+ --&gt; [0-9]+:[0-9]+:[0-9]+[,.][0-9]+)\n"
                        , "<time id='' stamp='$1'/>")
                .replaceAll("[ ]+", " ")
                .replaceAll("[\n]+", "\n");
    }

    public void processFile(String aPathSRT, String aPathTxt, String aLng) throws Exception {
        String aSrt = load(aPathSRT);
        String aTxt = load(aPathTxt);
        String aOut = processString(aSrt, aTxt, aLng);
        System.out.println("\n"
                + "Output (" + aPathTxt + ".srt" + "):");
        BufferedWriter aBW = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(aPathTxt + ".srt")
                        , StandardCharsets.UTF_8));
        aBW.write(aOut);
        aBW.flush();
        aBW.close();
    }

    public String processString(String aSRT, String aTxt, String aLng) throws Exception {
        if (_DEBUG_INOUT) {
            System.out.println("\nSRT: \n" + aSRT);
            System.out.println("\nTXT: \n" + aTxt);
        }
        String aSrtXml = toXml(aSRT);
        String aTxtXml = toXml(aTxt);
        if (_DEBUG_INOUT) {
            System.out.println("\nSRTXML: \n" + aSrtXml);
            System.out.println("\nTXTXML: \n" + aTxtXml);
        }

        String aCutOnRE = aLng.matches("(ja|zh|ko)") ? null : "[ \n]";

        TokenizerSimple aTokenizer = new TokenizerSimple();
        TokenizedSent aSrtTS = aTokenizer.tokenizeXmlSimple(aSrtXml, aCutOnRE);
        TokenizedSent aTxtTS = aTokenizer.tokenizeXmlSimple(aTxtXml, aCutOnRE);

        CubaixAlignerSimple aAligner = new CubaixAlignerSimple(true);
        TokenizedSent aSyncTS = aAligner.syncMarks1to2(aSrtTS, aTxtTS);

        final List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < aSyncTS.tokens.size()) {
            Token t = aSyncTS.tokens.get(i);
            if (t.kind == Token.NSTOKEN_KIND.MARK) {
                // Collect all consecutive MARKs
                int j = i + 1;
                while (j < aSyncTS.tokens.size()
                        && aSyncTS.tokens.get(j).kind == Token.NSTOKEN_KIND.MARK) {
                    j++;
                }
                // Keep ONLY the LAST MARK from this run
                tokens.add(aSyncTS.tokens.get(j - 1));
                i = j;
            } else {
                tokens.add(t);
                i++;
            }
        }

        StringBuilder aOut = new StringBuilder();
        StringBuilder aWaiting = new StringBuilder();
        int seq = 0;  // Renumber counter

        for (Token aT : tokens) {
            if (aT.kind == Token.NSTOKEN_KIND.MARK) {
                String aStamp = aT.getAttr("stamp");

                // Flush any waiting text BEFORE this mark
                if (!aWaiting.isEmpty()) {
                    String aPhrase = aWaiting.toString()
                            .replaceAll("&lt;", "<")
                            .replaceAll("&gt;", ">")
                            .trim() + "\n\n";
                    aOut.append(aPhrase);
                    if (_DEBUG_ALIGN) {
                        System.out.print(aPhrase);
                    }
                    aWaiting = new StringBuilder();
                }

                // Increment and use NEW sequential ID
                seq++;
                String aId = String.valueOf(seq);
                aOut.append(aId).append("\n").append(aStamp).append("\n");
                if (_DEBUG_ALIGN) {
                    System.out.print(aId + "\n" + aStamp + "\n");
                }
                continue;
            }
            aWaiting.append(aT.token);
        }

        // Flush remaining text after loop
        if (!aWaiting.isEmpty()) {
            String aPhrase = aWaiting.toString()
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .trim() + "\n\n";
            aOut.append(aPhrase);
            if (_DEBUG_ALIGN) {
                System.out.print(aPhrase);
            }
        }

        return aOut.toString();
    }

}
