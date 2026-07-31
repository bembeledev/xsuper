package com.dic.xsuper.render.javafx.tags.list;

import com.dic.xsuper.dom.node.XplNode;

/**
 * Tag HTML &lt;ol&gt; – lista ordenada.
 * Suporta: decimal, lower-alpha, upper-alpha, lower-roman, upper-roman.
 */
public class OlTag extends ListTag {

    public OlTag(XplNode node) {
        super(node);
    }

    @Override
    protected String getMarker(int index) {
        return switch (listStyleType) {
            case "lower-alpha" -> String.valueOf((char) ('a' + index - 1)) + ".";
            case "upper-alpha" -> String.valueOf((char) ('A' + index - 1)) + ".";
            case "lower-roman" -> toRoman(index).toLowerCase() + ".";
            case "upper-roman" -> toRoman(index).toUpperCase() + ".";
            default -> index + "."; // decimal
        };
    }

    private String toRoman(int n) {
        if (n <= 0) return "";
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] units = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return thousands[n / 1000] +
                hundreds[(n % 1000) / 100] +
                tens[(n % 100) / 10] +
                units[n % 10];
    }
}