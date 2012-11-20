package com.zipwhip.executors;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/12/12
* Time: 2:56 PM
*
* These are the common types of executors that our zipwhip-api uses.
* The ApiConnection uses BOSS/WORKER when you use AyncHttp, otherwise
* it just uses BOSS.
*
* The NettySignalConnection class (for signals) leverages all 3 when in
* NIO mode, otherwise just uses BOSS/EVENTS
*/
public enum CommonExecutorTypes {

    BOSS,

    WORKER,

    EVENTS
}
