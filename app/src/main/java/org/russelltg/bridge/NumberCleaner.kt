package org.russelltg.bridge

fun cleanNumber(i: String): String {

    // remove +1
    var ret = i.removePrefix("+1")

    // remove spaces and dashes and parens
    ret = ret.replace(" ", "")
    ret = ret.replace("-", "")
    ret = ret.replace("(", "")
    ret = ret.replace(")", "")

    return ret
}