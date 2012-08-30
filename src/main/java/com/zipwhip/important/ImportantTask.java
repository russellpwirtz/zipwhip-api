package com.zipwhip.important;

import java.io.Serializable;
import java.lang.String;import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 10:59 AM
 *
 * You would subclass this
 */
public interface ImportantTask<T extends Serializable> extends Serializable {

    // examples include: 'ConnectCommand'
    String getRequestType();

    // helps us keep track of this particular request through its lifecycle
    // usually this would be a guid or a clientId. Something to prevent dupes in requests.
    String getRequestId();

    // could be in the future, probably would be NOW() + 30 seconds
    Date getExpirationDate();

    // this would be custom parameters
    T getParameters();

}
