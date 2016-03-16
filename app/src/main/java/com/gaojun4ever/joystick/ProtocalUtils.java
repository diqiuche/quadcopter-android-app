package com.gaojun4ever.joystick;

/**
 * Created by gaojun4ever on 2016/2/28.
 */
public class ProtocalUtils {

    public static String adding_protocal(String str_input)
    {
        char[] asc_code_array = str_input.toCharArray();
        int checksume=0;
        //
        for(int i=0; i<asc_code_array.length; i++) checksume+=asc_code_array[i];
        checksume=256-(checksume&0xff);
        int checksum_low=checksume & 0x0f;
        int checksum_high=(checksume & 0x0f0)/16;
        if(checksum_low<10) checksum_low+=48;
        else checksum_low+=55;
        if(checksum_high<10) checksum_high+=48;
        else checksum_high+=55;
        //
        return new String(":"+str_input+(char)checksum_high+(char)checksum_low+"/");
    }

    public static String calculate_gas_str(int gas_value)
    {
        String bs005_gas_command_str=Integer.toHexString(gas_value).toUpperCase();
        if(bs005_gas_command_str.length()<2) bs005_gas_command_str="0"+bs005_gas_command_str;
        //
        return bs005_gas_command_str;
    }

}
