package com.munger.passwordkeeper;

import java.util.Random;

/**
 * Created by codymunger on 11/20/16.
 */

public class Helper
{
    public static char[] lowercase = new char[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    public static char[] uppercase = new char[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'Q', 'R', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    public static char[] numbers = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    public static char[] symbols = new char[] {'~','`','!','@','#','$','%','^','&','*','(',')','{','[','}',']','\\','|',':',';','"','\'','<',',','>','.','?','/'};

    public static String longString()
    {
        Random r = new Random();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 1024; i++)
        {
            int type = r.nextInt(4);
            int index = 0;

            switch(type)
            {
                case 0:
                    index = r.nextInt(lowercase.length);
                    b.append(lowercase[index]);
                    break;
                case 1:
                    index = r.nextInt(uppercase.length);
                    b.append(uppercase[index]);
                    break;
                case 2:
                    index = r.nextInt(numbers.length);
                    b.append(numbers[index]);
                    break;
                case 3:
                    index = r.nextInt(symbols.length);
                    b.append(symbols[index]);
                    break;
            }
        }

        return b.toString();
    }
}
