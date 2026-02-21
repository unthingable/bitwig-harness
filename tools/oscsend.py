#!/usr/bin/env python3
"""Minimal OSC sender. Replaces oscsend from liblo."""

import socket
import struct
import sys


def pad(data):
    """Pad bytes to 4-byte boundary."""
    remainder = len(data) % 4
    return data + b"\0" * (4 - remainder) if remainder else data


def encode_osc(address, type_args):
    """Encode an OSC message from address and (type, value) pairs."""
    msg = pad(address.encode() + b"\0")
    type_tag = "," + "".join(t for t, _ in type_args)
    msg += pad(type_tag.encode() + b"\0")
    for t, v in type_args:
        if t == "i":
            msg += struct.pack(">i", int(v))
        elif t == "f":
            msg += struct.pack(">f", float(v))
        elif t == "s":
            msg += pad(v.encode() + b"\0")
    return msg


def main():
    if len(sys.argv) < 3:
        print("Usage: oscsend.py <host> <port> <address> [type value ...]", file=sys.stderr)
        sys.exit(1)

    host = sys.argv[1]
    port = int(sys.argv[2])
    address = sys.argv[3]

    # Parse type-value pairs from remaining args
    type_args = []
    i = 4
    while i < len(sys.argv):
        t = sys.argv[i]
        if t in ("i", "f", "s") and i + 1 < len(sys.argv):
            type_args.append((t, sys.argv[i + 1]))
            i += 2
        else:
            print(f"Unknown type or missing value: {t}", file=sys.stderr)
            sys.exit(1)

    msg = encode_osc(address, type_args)
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(msg, (host, port))
    sock.close()


if __name__ == "__main__":
    main()
