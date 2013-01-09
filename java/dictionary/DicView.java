package dictionary;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * User: mityok
 * Date: 02.06.2008
 * Time: 22:51:25
 */
public class DicView {



  public static void main(String [] args) throws IOException {

    final String dicName = args[0];
    final String textName = args[1];

    final HashSet set = new HashSet(10000);

    final LineNumberReader text = new LineNumberReader(new FileReader(textName));

    String s = text.readLine();
    int wordcount = 0;
    while(s != null) {
      StringTokenizer tokenizer = new StringTokenizer(s, " ,;'./][{}())(*&^%$#!@`~-=+\"><\\");
      while (tokenizer.hasMoreTokens()) {
        final String  word = tokenizer.nextToken();

        wordcount ++;

        if (word.length() >= 4) {
          if (!set.contains(word))
            set.add(word);
        }
      }
      s = text.readLine();
    }

    System.out.println("Total words     = " + wordcount);
    System.out.println("Different words = " + set.size());
    System.out.println();

    for (Iterator i = set.iterator(); i.hasNext();) {
      final String word = (String) i.next();
      final LineNumberReader dic = new LineNumberReader(new FileReader(dicName));
      String s1;
      while ((s1 = dic.readLine()) != null) {
        final StringTokenizer tokenizer = new StringTokenizer(s1, " ,;'./][{}())(*&^%$#!@`~-=+\"><\\");

        boolean found = false;
        while(tokenizer.hasMoreTokens()) {
          String token  = tokenizer.nextToken();
          if (token.equalsIgnoreCase(word)) {
            System.out.println(s1);
            found = true;
            break;
          }
        }
        if (found)
          break;
      }
    }

  }

}
