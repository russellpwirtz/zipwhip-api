package com.zipwhip.api.signals.dto;

import com.zipwhip.util.Converter;
import com.zipwhip.util.DataConversionException;

import java.util.HashMap;

/**
 * Date: 9/10/13
 * Time: 2:41 PM
 *
 * @author Michael
 * @version 1
 */
public class SignalContactContentConverter implements Converter<SignalContact, HashMap<String, Object>> {

    @Override
    public HashMap<String, Object> convert(SignalContact signalContact) throws DataConversionException {
        return null;
    }
}
