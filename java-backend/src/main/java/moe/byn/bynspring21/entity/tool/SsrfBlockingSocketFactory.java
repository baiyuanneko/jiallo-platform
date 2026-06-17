package moe.byn.bynspring21.entity.tool;

import java.net.*;

public class SsrfBlockingSocketFactory {

    private SsrfBlockingSocketFactory() {}

    static boolean isBlockedAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress()) return true;                     // #6 0.0.0.0 / ::
        if (addr.isMulticastAddress()) return true;                   // 224.0.0.0/4, ff00::/8
        if (addr.isLoopbackAddress()) return true;
        if (addr.isLinkLocalAddress()) return true;
        if (addr.isSiteLocalAddress()) return true;
        if (addr instanceof Inet6Address) {
            byte[] bytes = addr.getAddress();
            if ((bytes[0] & 0xFE) == 0xFC) return true;                // ULA fc00::/7
            if (isIpv4Mapped(bytes)) {                                  // #3 ::ffff:x.x.x.x
                byte[] ipv4 = java.util.Arrays.copyOfRange(bytes, 12, 16);
                try {
                    return isBlockedAddress(InetAddress.getByAddress(ipv4));
                } catch (java.net.UnknownHostException e) {
                    return true;
                }
            }
        }
        if (addr instanceof Inet4Address) {
            byte[] b = addr.getAddress();
            int b0 = b[0] & 0xFF, b1 = b[1] & 0xFF;
            if (b0 == 0 && ((b[1] & 0xFF) != 0 || (b[2] & 0xFF) != 0 || (b[3] & 0xFF) != 0))
                return true;                                           // 0.0.0.0/8 (excl. 0.0.0.0)
            if (b0 >= 240) return true;                                // 240.0.0.0/4 (Class E + broadcast)
            if (b0 == 100 && b1 >= 64 && b1 <= 127) return true;      // CGNAT 100.64.0.0/10
            if (b0 == 168 && b1 == 63                                  // Azure WireServer
                    && (b[2] & 0xFF) == 129 && (b[3] & 0xFF) == 16) return true;

            // IANA Special-Purpose Address Registry (complete IPv4 coverage)
            if (b0 == 192 && b1 == 0 && (b[2] & 0xFF) == 0) return true;           // 192.0.0.0/24  IETF Protocol Assignments
            if (b0 == 192 && b1 == 0 && (b[2] & 0xFF) == 2) return true;           // 192.0.2.0/24  TEST-NET-1
            if (b0 == 192 && b1 == 88 && (b[2] & 0xFF) == 99) return true;         // 192.88.99.0/24  6to4 Relay Anycast
            if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;                  // 198.18.0.0/15  Benchmarking (RFC 2544)
            if (b0 == 198 && b1 == 51 && (b[2] & 0xFF) == 100) return true;        // 198.51.100.0/24  TEST-NET-2
            if (b0 == 203 && b1 == 0 && (b[2] & 0xFF) == 113) return true;         // 203.0.113.0/24  TEST-NET-3
        }
        return false;
    }

    private static boolean isIpv4Mapped(byte[] addr) {
        if (addr.length != 16) return false;
        for (int i = 0; i < 10; i++) if (addr[i] != 0) return false;
        return addr[10] == (byte) 0xFF && addr[11] == (byte) 0xFF;
    }
}
