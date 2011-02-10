#!/usr/bin/env python

import smtpd, asyncore

server = smtpd.DebuggingServer(('localhost', 25), None)
asyncore.loop()
