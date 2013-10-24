import os, sys, re, inspect, time
import cPickle as pickle

import matplotlib.pyplot as plt
import numpy as np

loaded = "micavis.ipython loaded %s" % time.ctime()

print "[micavis.ipython at %s]" % loaded

micavis_py_dir = os.path.split(__file__)[0]
mica_dir = os.path.split(os.path.split(micavis_py_dir)[0])[0]
ipython_datafile = os.path.join(mica_dir, 'micavis_ipython_export.pickle')

# stupid hack to avoid trying to pickle a function object...
_picklemode = False

# decorator to capture matplotlib plots in Micavis and export them to IPython
class snapshot(object):
    def __init__(self, func, datafile = ipython_datafile):
        self._func = None
        if isinstance(func, snapshot):
            func = func.func
        self.func = func
        self.datafile = datafile
    
    def __call__(self, *args,  **keywords):
        self.args = args
        self.keywords = keywords
        self.convert_args_to_keywords()
        self.save()
        return self.func(*self.args, **self.keywords)

    def convert_args_to_keywords(self):
        spec = inspect.getargspec(self.func)
        argnames, arglist_name, keywords_name, defaults = spec
        #print "spec:"
        #print argnames
        #print arglist_name
        #print keywords_name
        #print defaults

        n = len(argnames)
        named_args, anonymous_args = self.args[:n], self.args[n:]
        self.args = anonymous_args
        self.args_name = arglist_name
        self.keywords_name = keywords_name
        
        #print "named args supplied:", len(named_args)
        #print "anonysmous args supplied:", len(anonymous_args)
        #print "keywords supplied:", self.keywords.keys()

        for name, value in zip(argnames, named_args):
            #print "  arg -> keyword:", name
            self.keywords[name] = value
        
        default_names = argnames[-len(defaults):]
        for name, default_value in zip(default_names, defaults):
            if name not in self.keywords:
                #print "  default -> keyword:", name
                self.keywords[name] = default_value

        self.anonymous_keywords = {k:v for k, v in self.keywords.items() if k not in argnames}
        self.keywords = {k:v for k, v in self.keywords.items() if k in argnames}


    def replay(self, **newkeywords):
        kw = self.keywords.copy()
        kw.update(newkeywords)
        self.func(*self.args, **kw)

    @property
    def func(self):
        if _picklemode:
            return None

        if self._func is not None:
            return self._func
        
        # after loading pickled object, func == None...
        exec('import %s as fpackage' % self.func_package)
        exec('reload(fpackage)')
        self._func = eval('fpackage.%s.func' % self.func_name)
        return self._func

    @func.setter
    def func(self, func):
        import inspect
        self._func = func
        self.func_name = func.func_name
        self.func_package = 'micavis.ipython'
        self.func_code = inspect.getsourcelines(func)

    def saveas(self, filename):
        self.datafile = filename
        self.save()

    def save(self):
        global _picklemode
        _picklemode = True
        temp = self._func
        self._func = None
        with open(self.datafile,'wb') as f:
            pickle.dump(self, f)
        self._func = temp
        _picklemode = False

    @staticmethod
    def load(datafile = ipython_datafile):
        with open(datafile,'rb') as f:
            return pickle.load(f)

    def print_code(self):
        print ''.join(self.func_code[0])


    def print_code_body(self):
        temp = self.func_code[0][2:]
        whitespace_re = re.compile('^\s+')
        prefixes = [whitespace_re.search(line).group(0) for line in temp]
        prefixes.sort()
        indent_width = len(prefixes[0])
        dedented = [line[indent_width:] for line in temp]
        print ''.join(dedented)
        
    def get_code_header(self):
        from StringIO import StringIO
        buf = StringIO()

        if self.args_name:
            print >> buf, "%s = %s" % (self.args_name, repr(self.args))
        if self.keywords_name:
            print >> buf, "%s = %s" % (self.keywords_name, repr(self.anonymous_keywords))
        for k, v in sorted(self.keywords.items()):
            print >> buf, "%s = %s" % (k, repr(v))

        buf.seek(0)
        return buf.read()

    def print_code_header(self):
        print self.get_code_header()

    # namespace is a target for exec
    def exec_code_header(self, namespace):
        exec self.get_code_header() in  namespace


def load(datafile = ipython_datafile):
    return snapshot.load(datafile=datafile)


# plot multiple histograms
@snapshot
def analysis_plot_hist_multiple(datasets, xlabel="value", ylabel = "count", legends=None, nbins=100, normalize=False):
    fig = plt.figure()
    ax = fig.add_subplot(111)

    artists = []

    if legends is None:
        legends = [None] * len(datasets)
    assert(len(legends) == len(datasets))
    for data,label in zip(datasets,legends):
        if normalize:
            weights = data / float(sum(data))
        else:
            weights = None

        n,bins,patches = ax.hist(data, nbins, alpha=0.5, histtype='stepfilled',label=label,weights=weights)

    ax.grid(True)
    ax.set_ylabel(ylabel)
    ax.set_xlabel(xlabel)
    if legends and legends[0] != None:
        ax.legend()
    plt.show()

def analysis_plot_1d_curve(*matplotlib_args, **matplotlib_kw):
    fig = plt.figure()
    ax = fig.add_subplot(111)
    ax.plot(*matplotlib_args, **matplotlib_kw)
    plt.show()

# plot multiple curves
@snapshot
def analysis_plot_2d_multiple(xy_pairs, xlabel="x", ylabel = "y", legends=None):
    fig = plt.figure()
    ax = fig.add_subplot(111)
    artists = []
    for x,y in xy_pairs:
        artists += ax.plot(x,y)
    ax.grid(True)
    ax.axhline(0, color='black', lw=2)
    ax.set_ylabel(ylabel)
    ax.set_xlabel(xlabel)
    if legends:
        ax.legend(artists, legends)
    plt.show()
