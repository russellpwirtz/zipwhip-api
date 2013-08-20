package com.zipwhip.signals.presence;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 6/28/11
 * Time: 2:45 PM
 *
 * When they connect, they have to declare their category.
 *
 */
public enum UserAgentCategory implements Serializable {

    Phone,
    Browser,
    Tablet,
    Desktop,
    TV,
    Car,
    NONE,
    ALL

}
