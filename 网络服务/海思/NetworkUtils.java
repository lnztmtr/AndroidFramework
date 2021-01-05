/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Locale;
import android.os.SystemProperties; //for password encrypt property
import android.os.Parcel;
import android.util.Pair;


import android.util.Log;

/**
 * Native methods for managing network interfaces.
 *
 * {@hide}
 */
public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    /** Bring the named network interface up. */
    public native static int enableInterface(String interfaceName);

    /** Bring the named network interface down. */
    public native static int disableInterface(String interfaceName);

    public native static int checkIpConflict(String interfaceName, String ipAddr);

    /** Setting bit 0 indicates reseting of IPv4 addresses required */
    public static final int RESET_IPV4_ADDRESSES = 0x01;

    /** Setting bit 1 indicates reseting of IPv4 addresses required */
    public static final int RESET_IPV6_ADDRESSES = 0x02;

    /** Reset all addresses */
    public static final int RESET_ALL_ADDRESSES = RESET_IPV4_ADDRESSES | RESET_IPV6_ADDRESSES;

    /**
     * Reset IPv6 or IPv4 sockets that are connected via the named interface.
     *
     * @param interfaceName is the interface to reset
     * @param mask {@see #RESET_IPV4_ADDRESSES} and {@see #RESET_IPV6_ADDRESSES}
     */
    public native static int resetConnections(String interfaceName, int mask);

    /**
     * Start the DHCP client daemon, in order to have it request addresses
     * for the named interface, and then configure the interface with those
     * addresses. This call blocks until it obtains a result (either success
     * or failure) from the daemon.
     * @param interfaceName the name of the interface to configure
     * @param dhcpResults if the request succeeds, this object is filled in with
     * the IP address information.
     * @return {@code true} for success, {@code false} for failure
     */
    public static boolean runDhcp(String interfaceName, DhcpResults dhcpResults) {
        return runDhcpPlus(interfaceName,
                           dhcpResults,
                           0,
                           null,
                           null,
                           0,
                           null);
    }

    /**
     * Start the DHCP client daemon, in order to have it request addresses
     * for the named interface, and then configure the interface with those
     * addresses. This call blocks until it obtains a result (either success
     * or failure) from the daemon.
     * @param interfaceName the name of the interface to configure
     * @param dhcpResults if the request succeeds, this object is filled in with
     * the IP address information.
     * @param option60 the name of the interface to configure
     * @param login the name of the interface to configure
     * @param password the name of the interface to configure
     * @param option125 the name of the interface to configure
     * @param option125Info the name of the interface to configure
     * @return {@code true} for success, {@code false} for failure
     */
    public native static boolean runDhcpPlus(String interfaceName, DhcpResults dhcpResults,
                                             int option60, String login, String password,
                                             int option125, String option125Info);

    /**
     * Initiate renewal on the Dhcp client daemon. This call blocks until it obtains
     * a result (either success or failure) from the daemon.
     * @param interfaceName the name of the interface to configure
     * @param dhcpResults if the request succeeds, this object is filled in with
     * the IP address information.
     * @return {@code true} for success, {@code false} for failure
     */
    public native static boolean runDhcpRenew(String interfaceName, DhcpResults dhcpResults);

    /**
     * Shut down the DHCP client daemon.
     * @param interfaceName the name of the interface for which the daemon
     * should be stopped
     * @return {@code true} for success, {@code false} for failure
     */
    public native static boolean stopDhcp(String interfaceName);

    /**
     * Release the current DHCP lease.
     * @param interfaceName the name of the interface for which the lease should
     * be released
     * @return {@code true} for success, {@code false} for failure
     */
    public native static boolean releaseDhcpLease(String interfaceName);

    /**
     * Start the DHCPv6 client daemon, in order to have it request addresses
     * for the named interface, and then configure the interface with those
     * addresses. This call blocks until it obtains a result (either success
     * or failure) from the daemon.
     * @param interfaceName the name of the interface to configure
     * @return ipv6 address for success, null for failure
     */
    //public native static boolean runDhcpv6(String interfaceName);
    public native static boolean runDhcpv6Plus(String interfaceName,
                                             int option60, String login, String password,
                                             int option125, String option125Info);

    public static boolean runDhcpv6(String interfaceName) {
        return runDhcpv6Plus(interfaceName,
                           0,
                           null,
                           null,
                           0,
                           null);
    }

    /**
     * TODO
     * add for get dns
     *
     */
     public native static boolean runDhcpv6Dns(String interfaceName);


    /**
     * TODO
     * add for release
     *
     */
     public native static boolean releaseDhcpv6lease(String interfaceName);


    /**
     * Shut down the DHCPv6 client daemon.
     * @param interfaceName the name of the interface for which the daemon
     * should be stopped
     * @return {@code true} for success, {@code false} for failure
     */
    public native static boolean stopDhcpv6(String interfaceName);

    /**
     * check DHCPv6 status
     * @param interfaceName the name of the interface
     * be released
     * @return {@code true} for success, {@code false} for failure
     */
    public native static boolean checkDhcpv6Status(String interfaceName);

    /**
     * get DHCPv6 ipaddress
     * @param interfaceName the name of the interface
     * @return ipv6 address for success, null for failure
     */
    public native static String getDhcpv6Ipaddress(String interfaceName);

    /**
     * get IPv6 Link-local ipaddress
     * @param interfaceName the name of the interface
     * @return ipv6 address for success, null for failure
     */
    public native static String getIpv6LinklocalAddress(String interfaceName);

    /**
     * get DHCPv6 gateway
     * @param null
     * @return ipv6 gateway for success, null for failure
     */
    public native static String getDhcpv6Gateway(String interfaceName);

    /**
     * get DHCPv6 prefixlen
     * @param interfaceName the name of the interface
     * @return ipv6 prefixlen for success, null for failure
     */
    public native static String getDhcpv6Prefixlen(String interfaceName);

    /**
     * get DHCPv6 dns
     * @param interfaceName the name of the interface
     * @param cnt the number of dns, 1 for primary dns, 2 for secondary dns
     * @return ipv6 dns for success, null for failure
     */
    public native static String getDhcpv6Dns(String interfaceName, int cnt);

    /**
     * get DHCPv6 dns count
     * @param interfaceName the name of the interface
     * @return ipv6 dns count
     */
    public native static int getDhcpv6DnsCnt(String interfaceName);

    /**
     * Return the last DHCPv6-related error message that was recorded.
     * <p/>NOTE: This string is not localized, but currently it is only
     * used in logging.
     * @return the most recent error message, if any
     */
    public native static String getDhcpv6Error();

    /**
     * Configure interface by Static IPV6
     * @param interfaceName
     * @param ipAddress
     * @param prefixLength
     * @param gateway
     * @param dns1
     * @param dns2
     * @return true for success, false for fail
     */
    public native static boolean configure6Interface(
        String interfaceName, String ipAddress, int prefixLength, String gateway, String dns1, String dns2);

    /**
     * Clear one specified interface IPV6 address
     * @param interfaceName
     * @param addr
     * @return true for success, false for fail
     */
    public native static boolean clearIpv6SpecifiedAddresses(String interfaceName, String addr);

    public static boolean removeIpv6Addresses(String interfaceName, String addr) {
        if (interfaceName == null || addr == null)
           return false;
        if(addr.indexOf('/') > -1) {
            addr = addr.substring(0, addr.indexOf('/'));
        }
        return clearIpv6SpecifiedAddresses(interfaceName, addr);
    }

    /**
     * Clear interface IPV6 address
     * @param interfaceName
     * @return true for success, false for fail
     */
    public native static boolean clearIpv6Addresses(String interfaceName);

    /**
     * Clear interface IPV4 address
     * @param interfaceName
     * @return true for success, false for fail
     */
    public native static boolean clearIpv4Addresses(String interfaceName);

    /**
     * Return the last DHCP-related error message that was recorded.
     * <p/>NOTE: This string is not localized, but currently it is only
     * used in logging.
     * @return the most recent error message, if any
     */
    public native static String getDhcpError();

    /**
     * Set the SO_MARK of {@code socketfd} to {@code mark}
     */
    public native static void markSocket(int socketfd, int mark);

    /**
     *Return the ip addresss of the interface.
     */
    public native static String getIpaddr(String interfaceName);

    /**
     *Return the netmask addresss of the interface.
     */
    public native static String getNetmask(String interfaceName);

    public native static int getNetlinkStatus(String interfaceName);

    /** Remove the default route for the named interface. */
    public native static int removeDefaultRoute(String interfaceName);

    public native static int removeNetRoute(String interfaceName);
    /** Reset any sockets that are connected via the named interface. */

    /**
     * When static IP configuration has been specified, configure the network
     * interface according to the values supplied.
     * @param interfaceName the name of the interface to configure
     * @param ipInfo the IP address, default gateway, and DNS server addresses
     * with which to configure the interface.
     * @return {@code true} for success, {@code false} for failure
     */
    public static boolean configureInterface(String interfaceName, DhcpInfo ipInfo) {
        return configureNative(interfaceName,
            ipInfo.ipAddress,
            ipInfo.netmask,
            ipInfo.gateway,
            ipInfo.dns1,
            ipInfo.dns2);
    }

    private native static boolean configureNative(
        String interfaceName, int ipAddress, int netmask, int gateway, int dns1, int dns2);

    /** Return the gateway address for the default route for the named interface. */
    public native static int getDefaultRoute(String interfaceName);

    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     * @param hostAddress an int corresponding to the IPv4 address in network byte order
     */
    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                                (byte)(0xff & (hostAddress >> 8)),
                                (byte)(0xff & (hostAddress >> 16)),
                                (byte)(0xff & (hostAddress >> 24)) };

        try {
           return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
           throw new AssertionError();
        }
    }

    /**
     * Convert a IPv4 address from an InetAddress to an integer
     * @param inetAddr is an InetAddress corresponding to the IPv4 address
     * @return the IP address as an integer in network byte order
     */
    public static int inetAddressToInt(InetAddress inetAddr)
            throws IllegalArgumentException {
        byte [] addr = inetAddr.getAddress();
        if (addr.length != 4) {
            throw new IllegalArgumentException("Not an IPv4 address");
        }
        return ((addr[3] & 0xff) << 24) | ((addr[2] & 0xff) << 16) |
                ((addr[1] & 0xff) << 8) | (addr[0] & 0xff);
    }

    /**
     * Convert a network prefix length to an IPv4 netmask integer
     * @param prefixLength
     * @return the IPv4 netmask as an integer in network byte order
     */
    public static int prefixLengthToNetmaskInt(int prefixLength)
            throws IllegalArgumentException {
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("Invalid prefix length (0 <= prefix <= 32)");
        }
        int value = 0xffffffff << (32 - prefixLength);
        return Integer.reverseBytes(value);
    }

    /**
     * Convert a IPv4 netmask integer to a prefix length
     * @param netmask as an integer in network byte order
     * @return the network prefix length
     */
    public static int netmaskIntToPrefixLength(int netmask) {
        return Integer.bitCount(netmask);
    }

    /**
     * Create an InetAddress from a string where the string must be a standard
     * representation of a V4 or V6 address.  Avoids doing a DNS lookup on failure
     * but it will throw an IllegalArgumentException in that case.
     * @param addrString
     * @return the InetAddress
     * @hide
     */
    public static InetAddress numericToInetAddress(String addrString)
            throws IllegalArgumentException {
        return InetAddress.parseNumericAddress(addrString);
    }

    /**
     * Writes an InetAddress to a parcel. The address may be null. This is likely faster than
     * calling writeSerializable.
     */
    protected static void parcelInetAddress(Parcel parcel, InetAddress address, int flags) {
        byte[] addressArray = (address != null) ? address.getAddress() : null;
        parcel.writeByteArray(addressArray);
    }

    /**
     * Reads an InetAddress from a parcel. Returns null if the address that was written was null
     * or if the data is invalid.
     */
    protected static InetAddress unparcelInetAddress(Parcel in) {
        byte[] addressArray = in.createByteArray();
        if (addressArray == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(addressArray);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Get InetAddress masked with prefixLength.  Will never return null.
     * @param IP address which will be masked with specified prefixLength
     * @param prefixLength the prefixLength used to mask the IP
     */
    public static InetAddress getNetworkPart(InetAddress address, int prefixLength) {
        if (address == null) {
            throw new RuntimeException("getNetworkPart doesn't accept null address");
        }

        byte[] array = address.getAddress();

        if (prefixLength < 0 || prefixLength > array.length * 8) {
            throw new RuntimeException("getNetworkPart - bad prefixLength");
        }

        int offset = prefixLength / 8;
        int reminder = prefixLength % 8;
        byte mask = (byte)(0xFF << (8 - reminder));

        if (offset < array.length) array[offset] = (byte)(array[offset] & mask);

        offset++;

        for (; offset < array.length; offset++) {
            array[offset] = 0;
        }

        InetAddress netPart = null;
        try {
            netPart = InetAddress.getByAddress(array);
        } catch (UnknownHostException e) {
            throw new RuntimeException("getNetworkPart error - " + e.toString());
        }
        return netPart;
    }

    /**
     * Check if IP address type is consistent between two InetAddress.
     * @return true if both are the same type.  False otherwise.
     */
    public static boolean addressTypeMatches(InetAddress left, InetAddress right) {
        return (((left instanceof Inet4Address) && (right instanceof Inet4Address)) ||
                ((left instanceof Inet6Address) && (right instanceof Inet6Address)));
    }

    /**
     * Convert a 32 char hex string into a Inet6Address.
     * throws a runtime exception if the string isn't 32 chars, isn't hex or can't be
     * made into an Inet6Address
     * @param addrHexString a 32 character hex string representing an IPv6 addr
     * @return addr an InetAddress representation for the string
     */
    public static InetAddress hexToInet6Address(String addrHexString)
            throws IllegalArgumentException {
        try {
            return numericToInetAddress(String.format(Locale.US, "%s:%s:%s:%s:%s:%s:%s:%s",
                    addrHexString.substring(0,4),   addrHexString.substring(4,8),
                    addrHexString.substring(8,12),  addrHexString.substring(12,16),
                    addrHexString.substring(16,20), addrHexString.substring(20,24),
                    addrHexString.substring(24,28), addrHexString.substring(28,32)));
        } catch (Exception e) {
            Log.e("NetworkUtils", "error in hexToInet6Address(" + addrHexString + "): " + e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create a string array of host addresses from a collection of InetAddresses
     * @param addrs a Collection of InetAddresses
     * @return an array of Strings containing their host addresses
     */
    public static String[] makeStrings(Collection<InetAddress> addrs) {
        String[] result = new String[addrs.size()];
        int i = 0;
        for (InetAddress addr : addrs) {
            result[i++] = addr.getHostAddress();
        }
        return result;
    }

    /**
     * Trim leading zeros from IPv4 address strings
     * Our base libraries will interpret that as octel..
     * Must leave non v4 addresses and host names alone.
     * For example, 192.168.000.010 -> 192.168.0.10
     * TODO - fix base libraries and remove this function
     * @param addr a string representing an ip addr
     * @return a string propertly trimmed
     */
    public static String trimV4AddrZeros(String addr) {
        if (addr == null) return null;
        String[] octets = addr.split("\\.");
        if (octets.length != 4) return addr;
        StringBuilder builder = new StringBuilder(16);
        String result = null;
        for (int i = 0; i < 4; i++) {
            try {
                if (octets[i].length() > 3) return addr;
                builder.append(Integer.parseInt(octets[i]));
            } catch (NumberFormatException e) {
                return addr;
            }
            if (i < 3) builder.append('.');
        }
        result = builder.toString();
        return result;
    }

    // 加密的密钥，构造一定长度的字节数组
    private final static byte[] KEY_BYTES = "Hi6si0li7con1For2Chi8na3Set4Top5Bo9x".getBytes();//"Vp6flFpGW86g7hi6MhD3Zl2eThJTjPnIjXE4".getBytes();
    private final static int KEY_LENGTH = KEY_BYTES.length;

    /**
     * 异或运算加密
     *
     * @param input 要加密的内容
     * @return 加密后的数据
     */
    public static byte[] xorEncode(byte[] input) {
        int keyIndex = 0;
        int length = input.length;
        for (int i = 0; i < length; i++) {
            input[i] = (byte) (input[i] ^ KEY_BYTES[(keyIndex++ % KEY_LENGTH)]);
        }
        return input;
    }

    /**
     * 异或运算解密
     *
     * @param input 要解密的内容
     * @return 解密后的数据
     */
    public static byte[] xorDecode(byte[] input) {
        int keyIndex = 0;
        int length = input.length;
        for (int i = 0; i < length; i++) {
            input[i] = (byte) (input[i] ^ KEY_BYTES[(keyIndex++ % KEY_LENGTH)]);
        }
        return input;
    }

    public static String passwdEncode(String input) {
        if (input == null )
           return null;

        if (SystemProperties.get("sys.ethernet.password.encrypt", "false").equals("false")){//don't encrypt
           return input;
        }

        String afterEncodeString = new String(xorEncode(input.getBytes()));
        return afterEncodeString;
    }

    public static String passwdDecode(String input) {
        if (input == null )
           return null;

        if (SystemProperties.get("sys.ethernet.password.encrypt", "false").equals("false")){//don't decrypt
           return input;
        }

        String orgString = new String(xorDecode(input.getBytes()));
        return orgString;
    }

}
