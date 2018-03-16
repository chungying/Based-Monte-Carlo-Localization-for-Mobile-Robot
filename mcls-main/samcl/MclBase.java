package samcl;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FloatConverter;

public class MclBase{	
	public void setupMclBase(MclBase other) {
		this.ifShowSER         = other.ifShowSER        ;
		this.ifShowParticles   = other.ifShowParticles  ;
		this.ifShowSensors     = other.ifShowSensors    ;
		this.convergeFlag      = other.convergeFlag     ;
		this.ignore            = other.ignore           ;
		this.debugMode         = other.debugMode        ;
		this.safe_edge         = other.safe_edge        ;
		this.period            = other.period           ;
		this.resampleInterval  = other.resampleInterval ;
		this.Nt                = other.Nt               ;
		this.tournamentPresure = other.tournamentPresure;
		this.deltaEnergy       = other.deltaEnergy      ;
		this.XI                = other.XI               ;
		this.ALPHA             = other.ALPHA            ;
		this.tableName         = other.tableName        ;
		this.dThresh           = other.dThresh          ;
		this.aThresh           = other.aThresh          ;
	}

	//parameters whose initial states have to be cached  
	@Parameter(names = {"-ss", "--showSer"}, description = "if TRUE, display Similar Energy Region (SER), default is false", required = false, arity = 1)
	public boolean ifShowSER = false;

	@Parameter(names = {"-sp", "--showParticles"}, description = "if TRUE, display particles, default is false", required = false, arity = 1)
	public boolean ifShowParticles = false;

	@Parameter(names = {"-sl", "--showLaserhits"}, description = "if TRUE, display laser hits, default is false", required = false, arity = 1)
	public boolean ifShowSensors = false;

	@Parameter(names = {"-msr","--mapSafeRange"}, description = "the range of map edge which wouldn't be used during the process, must be greater than 1, default is 10 pixel.", required = false, arity = 1)
	public int safe_edge = 10;//TODO change variable name

	@Parameter(names = {"-c","--converge"}, description = "if TRUE, initial robot pose is known for MCL, default is false", required = false, arity = 1)
	public boolean convergeFlag = false;

	@Parameter(names = {"-inl", "--ignoreNetworkLatency"}, description = "if TRUE, report ignores network latency, default is false", required = false, arity = 1)
	public boolean ignore = false;//TODO change variable name
	
	@Parameter(names = {"-D","--debug"}, description = "start up/stop debug mode, default is false", required = false, arity = 1)
	public boolean debugMode = false;
	
	@Parameter(names = {"-dms", "--delayMilliSecond"}, description = "delay time for each iteration, default is 0", required = false, arity = 1)
	protected int period = 0;//TODO change variable name
	
	@Parameter(names = {"-ed","--energyDelta"}, description = "delta of Similar Energy Region(SER), default is 0.01", required = false, arity = 1, converter = FloatConverter.class)
	public float deltaEnergy = (float)0.01;//for Caculating_SER()//TODO change variable name
	
	@Parameter(names = {"-xi","--sensitiveCoefficient"}, description = "sensitive coefficient for detecting robot kidnapped problem, default is 0.1", required = false, arity = 1, converter = FloatConverter.class)
	public float XI = (float)0.1;//for Determining_size()//TODO change variable name
	
	@Parameter(names = {"-pr","--populationRatio"}, description = "the ratio of sizes of global and local particle sets, default is 0.6", required = false, arity = 1, converter = FloatConverter.class)
	public float ALPHA = (float)0.6;//TODO change variable name
	
	@Parameter(names = {"-ri", "--resampleInterval"}, description = "the interval of resmapling, default is 1.", required = false, arity = 1)
	public int resampleInterval = 1;
	
	@Parameter(names = {"-pn","--particleNumber"}, description = "the number of total population, default is 100 particles.", required = true, arity = 1)
	public int Nt = 100;//TODO change variable name

	@Parameter(names = {"-tn","--tableName"}, description = "the name of HBase table, default is \"map.512.4.split\"", required = false, arity = 1)
	public String tableName = "map.512.4.split";
	
	@Parameter(names = {"-tp","--tournamentPresure"}, description = "tournament selection presure for samcl, default is 10 particles.", required = false, arity = 1)
	protected int tournamentPresure = 10;
	
	@Parameter(names = {"-md", "--minimumDistance"}, description = "threshold of distance difference for updating MCL", 	required = false, arity = 1)
	protected double dThresh = 0.001;
	
	@Parameter(names = {"-ma", "--minimumAngle"}, description = "threshold of distance difference for updating MCL", required = false, arity = 1)
	protected double aThresh = 0.001;
}
