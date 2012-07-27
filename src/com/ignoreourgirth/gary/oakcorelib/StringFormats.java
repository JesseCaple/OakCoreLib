/*******************************************************************************
 * Copyright (c) 2012 GaryMthrfkinOak (Jesse Caple).
 * 
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ignoreourgirth.gary.oakcorelib;

import java.text.DecimalFormat;

public final class StringFormats {

	protected StringFormats() {}
	
	protected static DecimalFormat decimalFormater = new DecimalFormat("#,###");
	protected static DecimalFormat moneyFormater = new DecimalFormat("#,###.00");
	
    enum Numeral {
        I(1), IV(4), V(5), IX(9), X(10), XL(40), L(50), XC(90), C(100), CD(400), D(500), CM(900), M(1000);
        int weigth;

        Numeral(int weigth) {
            this.weigth = weigth;
        }
    };

    public static String toRomanNumeral(long n) {

        if( n <= 0) {
            throw new IllegalArgumentException();
        }

        StringBuilder returnValue = new StringBuilder();

        final Numeral[] values = Numeral.values();
        for (int i = values.length - 1; i >= 0; i--) {
            while (n >= values[i].weigth) {
                returnValue.append(values[i]);
                n -= values[i].weigth;
            }
        }
        return returnValue.toString();
    }
    
    public static String toCurrency(double money) {
    	return toCurrency(money, true);
    }
    
    public static String toCurrency(double money, boolean appendCurrencyName) {
    	StringBuilder returnValue = new StringBuilder();
    	if (money == 0) {
    		returnValue.append("0.00 ");
    	} else {
    		returnValue.append(moneyFormater.format(money));
    		returnValue.append(' ');
    	}
    	if (appendCurrencyName) {
        	if (money != 1) {
        		returnValue.append(OakCoreLib.getEconomy().currencyNamePlural());
        	} else {
        		returnValue.append(OakCoreLib.getEconomy().currencyNameSingular());
        	}
    	}
    	return returnValue.toString();
    }
    
    public static String formatInteger(int number) {
    	if (number == 0) {
    		return "0";
    	} else {
    		return decimalFormater.format(number);
    	}
    }

}
