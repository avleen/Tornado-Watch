#!/usr/bin/python

import os


class Pidlock:
    def __init__(self, appname):
        self.appname = appname
        self.pidfile = '/tmp/%s.pid' % self.appname
        self.mypid = os.getpid()

    def start(self):
        if os.path.exists(self.pidfile):
            old_pid = open(self.pidfile).read()
            try:
                os.kill(int(old_pid), 0)
            except:
                pass
            else:
                raise PidExists("PID %s is running as %s" % (old_pid, self.appname))
        open(self.pidfile, 'w').write(str(self.mypid))

    def stop(self):
        if os.path.exists(self.pidfile):
            old_pid = open(self.pidfile).read()
            if int(old_pid) == self.mypid:
                os.unlink(self.pidfile)


class PidExists(Exception):
    def __init__(self, value):
        self.parameter = value
    def __str__(self):
        return repr(self.parameter)
