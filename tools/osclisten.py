#!/usr/bin/env python3
"""Dual-stack (IPv4+IPv6) OSC listener. Replaces oscdump for testing."""

import socket
import struct
import sys


def parse_osc_message(data):
    """Parse an OSC message into (address, type_tag, args)."""
    # Address string (null-terminated, padded to 4 bytes)
    end = data.index(0)
    address = data[:end].decode()
    offset = end + (4 - end % 4) if end % 4 != 0 else end + 4

    if offset >= len(data):
        return address, ",", []

    # Type tag string
    end = data.index(0, offset)
    type_tag = data[offset:end].decode()
    offset = end + (4 - end % 4) if end % 4 != 0 else end + 4

    # Arguments
    args = []
    for t in type_tag[1:]:  # skip leading comma
        if t == "i":
            args.append(struct.unpack(">i", data[offset : offset + 4])[0])
            offset += 4
        elif t == "f":
            args.append(struct.unpack(">f", data[offset : offset + 4])[0])
            offset += 4
        elif t == "s":
            end = data.index(0, offset)
            args.append(data[offset:end].decode())
            offset = end + (4 - end % 4) if end % 4 != 0 else end + 4
        elif t == "h":
            args.append(struct.unpack(">q", data[offset : offset + 8])[0])
            offset += 8
        elif t == "d":
            args.append(struct.unpack(">d", data[offset : offset + 8])[0])
            offset += 8
        else:
            args.append(f"<{t}?>")
    return address, type_tag, args


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9001

    sock = socket.socket(socket.AF_INET6, socket.SOCK_DGRAM)
    sock.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_V6ONLY, 0)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(("::", port))
    print(f"Listening on [::]:{port} (dual-stack IPv4+IPv6)", file=sys.stderr)

    try:
        while True:
            data, addr = sock.recvfrom(65536)
            try:
                address, type_tag, args = parse_osc_message(data)
                args_str = " ".join(str(a) for a in args)
                print(f"{address} {type_tag} {args_str}", flush=True)
            except Exception as e:
                print(f"<parse error: {e}, {len(data)} bytes from {addr}>", flush=True)
    except KeyboardInterrupt:
        pass
    finally:
        sock.close()


if __name__ == "__main__":
    main()
