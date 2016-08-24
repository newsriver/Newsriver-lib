package ch.newsriver.util.text;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.regex.Pattern;

/**
 * Created by eliapalme on 11/03/16.
 */
public class TextNormaliser {

    //do not use \\w as it doese not contain letter with accents instead we are using \p{L}
    //\uD83C-\uDBFF\uDC00-\uDFFF This is for emoji
    private static final Pattern onlyLetters = Pattern.compile("[^\\p{L}]");

    private static final Pattern multiPoints = Pattern.compile("\\.+");
    private static final Pattern multiSpaces = Pattern.compile("\\s+");
    private static final Pattern noTags = Pattern.compile("(<([^>]+)>)");
    //\uD83C-\uDBFF\uDC00-\uDFFF This is for emoji
    private static final Pattern onlyLettersAndSymbols = Pattern.compile("[^\\p{L}\\p{N}\\p{M}\\p{P}\\p{S}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]");

    public static String cleanText(String text) {

        // Unescape html encoded chars
        text = StringEscapeUtils.unescapeHtml4(text);

        //Remove all html taga e.g.: <b> </b>


        text = noTags.matcher(new InterruptibleCharSequence(text)).replaceAll("");
        text = onlyLettersAndSymbols.matcher(new InterruptibleCharSequence(text)).replaceAll(" ");
        text = multiPoints.matcher(new InterruptibleCharSequence(text)).replaceAll(".");
        text = multiSpaces.matcher(new InterruptibleCharSequence(text)).replaceAll(" ");


        text = text.trim();

        return text;

    }


    public static String caseNormalizer(String text) {

        int upperCount = 0;
        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) {
                upperCount++;
            }
        }

        if (upperCount > text.length() / 2) {
            text = text.toLowerCase();
            text = WordUtils.capitalize(text, '.');
        }


        return text;
    }


    public static String cleanAndremoveSymbols(String text) {

        text = onlyLetters.matcher(new InterruptibleCharSequence(text)).replaceAll(" ");
        text = multiSpaces.matcher(new InterruptibleCharSequence(text)).replaceAll(" ");

        text = text.trim();

        return text;

    }
}
