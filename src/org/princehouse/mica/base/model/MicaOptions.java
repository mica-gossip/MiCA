package org.princehouse.mica.base.model;

import java.util.List;

import org.princehouse.mica.util.Functional;

import com.beust.jcommander.Parameter;

public class MicaOptions {

    @Parameter(names = "-stagger", description = "amount of time (ms) to stagger starting runtimes")
    public int stagger = 10000;

    @Parameter(names = { "-expname" }, description = "Short experiment name")
    public String expname = "n";

    @Parameter(names = { "-logprefix" }, description = "Prefix for logfilenames")
    public String logprefix = "";

    @Parameter(names = { "-log" }, description = "CSV Log file location (deprecated)")
    public String logfile = "mica.log";

    @Parameter(names = { "-logdir" }, description = "JSON log directory.  Default ./mica_log")
    public String logdir = "mica_log";

    @Parameter(names = { "-clearlogdir" }, description = "Delete pre-existing log files in logdir")
    public Boolean clearLogdir = true;

    @Parameter(names = "-n", description = "Number of nodes to run")
    public Integer n = 25;

    @Parameter(names = "-rdegree", description = "Degree of nodes in random graph. (Currently must be even).  Only used for graphType=random")
    public Integer rdegree = 4;

    @Parameter(names = "-port", description = "Starting port")
    public Integer port = 8000;

    @Parameter(names = "-host", description = "Host")
    public String host = "localhost";

    @Parameter(names = "-seed", description = "Random seed")
    public Long seed = 0L;

    @Parameter(names = "-round", description = "Round length (ms)")
    public int roundLength = 5000;

    // contentionBackoff = 1.0 means that we wait up to 1 round (rate adjusted) if our connection lock times out
    @Parameter(names = "-contentionBackoff", description = "Max wait time for contention backoff as a fraction of rate-adjusted round length")
    public double contentionBackoff = 0.5;
    
    @Parameter(names = "-stopAfter", description = "Halt simulation after this many rounds (0 = run forever)")
    public double stopAfter = 0;

    @Parameter(names = "-graphType", description = "Type of communication graph to use. Valid options: random, complete, singlering")
    public String graphType = "random";

    @Parameter(names = "-implementation", description = "Runtime implementation name. Valid options: simple, sim.  Default: sim")
    public String implementation = "sim";

    @Parameter(names = "-compiler", description = "Analysis implementation. Options: default, simple, fake, c1")
    public String compiler = "default";

    @Parameter(names = "-timeout", description = "Lock waiting timeout (ms)")
    public int timeout = 5000;

    @Parameter(names = "-ldisable", variableArity = true, description = "Log types to disable (space separated log names). See LogFlag enum for log names.")
    public List<String> logsDisable = Functional.list();

    @Parameter(names = "-lenable", variableArity = true, description = "Log types to enable (space separated log names). See LogFlag enum for log names.")
    public List<String> logsEnable = Functional.list();

    @Parameter(names = "-cacheReflection", description = "(Expert) Cache foreign objects analysis. This will cause incorrect behavior if new sub-protocols created dynamically after initialization of the parent protocol.")
    public Boolean reflectionCache = false;

    @Parameter(names = "-simUpdateDuration", description = "Simulator only. Duration (ms) of simuated update function execution.  -1 indicates real wall clock time (default).")
    public int simUpdateDuration = -1;

    @Parameter(names = "-serializer", description = "Default serializer. Values are 'java' or 'kryo'")
    public String serializer = "java";

    @Parameter(names = "-logErrorLocations", description = "Record error locations in the logs, default false")
    public boolean logErrorLocations = true;

    public String mainClassName = null;
}
