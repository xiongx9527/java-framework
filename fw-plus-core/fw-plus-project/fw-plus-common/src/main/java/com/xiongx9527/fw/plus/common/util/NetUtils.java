package com.xiongx9527.fw.plus.common.util;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * NetUtils
 * 
 * @author yutianbao
 */
@Slf4j
public abstract class NetUtils {

    /**
     * Pre-loaded local address
     */
    public static InetAddress localAddress;

    static {
        try {
            localAddress = getLocalInetAddress();
        } catch (SocketException e) {
            throw new RuntimeException("fail to get local ip.");
        }
    }

    /**
     * Retrieve the first validated local ip address(the Public and LAN ip addresses are validated).
     *
     * @return the local address
     * @throws SocketException the socket exception
     */
    public static InetAddress getLocalInetAddress() throws SocketException {
        // enumerates all network interfaces
        Enumeration<NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();

        while (enu.hasMoreElements()) {
            NetworkInterface ni = enu.nextElement();
            if (ni.isLoopback()) {
                continue;
            }

            Enumeration<InetAddress> addressEnumeration = ni.getInetAddresses();
            while (addressEnumeration.hasMoreElements()) {
                InetAddress address = addressEnumeration.nextElement();

                // ignores all invalidated addresses
                if (address.isLinkLocalAddress() || address.isLoopbackAddress() || address.isAnyLocalAddress()) {
                    continue;
                }

                return address;
            }
        }

        throw new RuntimeException("No validated local address!");
    }

    /**
     * Retrieve local address
     * 
     * @return the string local address
     */
    public static String getLocalAddress() {
        return localAddress.getHostAddress();
    }

    /**
     * 获取本机MAC地址的简短标识
     * 优先获取本地主机对应网络接口的硬件地址，并返回其后8位字符
     * 如果获取失败或MAC地址不存在，则返回"unknown"
     * 
     * @return MAC地址的后8位字符，格式为XX-XX-XX-XX；如果获取失败则返回"unknown"
     */
    public static String getMacAddress() {
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            java.net.NetworkInterface networkInterface = java.net.NetworkInterface.getByInetAddress(localHost);
            if (networkInterface != null) {
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        if (i != 0) {
                            sb.append("-");
                        }
                        sb.append(String.format("%02X", mac[i]));
                    }
                    String macStr = sb.toString();
                    return macStr.length() > 8 ? macStr.substring(macStr.length() - 8) : macStr;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get MAC address, using 'unknown' as fallback", e);
        }
        return "unknown";
    }

}