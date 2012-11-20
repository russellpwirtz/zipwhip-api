package com.zipwhip.api.connection;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 11/14/12
 * Time: 11:19 AM
 */
public interface RequestBody {

    InputStream toStream();

}
