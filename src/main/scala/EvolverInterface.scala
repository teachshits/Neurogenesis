package neurogenesis.doubleprecision

import neurogenesis.msg._
import neurogenesis.util._

import scala.swing.SimpleSwingApplication
import scala.swing._
import scala.swing.FileChooser.SelectionMode
import scala.swing.event._
import scala.util.Random
import scala.xml._
import java.io.File
import java.io.FileWriter
import java.io.BufferedReader
import java.io.FileReader
import scala.xml.Text
import scalala.library.Plotting
import scalala.generic.collection.CanViewAsTensor1._
import javax.swing.border._
import edu.uci.ics.jung.graph.SparseGraph
import edu.uci.ics.jung.io.GraphMLWriter
//import edu.uci.ics.jung.algorithms.layout.CircleLayout
import edu.uci.ics.jung.algorithms.layout.FRLayout
import edu.uci.ics.jung.visualization.VisualizationImageServer
import java.awt.Image
import java.awt.image.RenderedImage
import java.awt.Point
import javax.imageio.ImageIO
import java.awt.Graphics2D
import javax.swing.JFrame
import javax.swing.JPanel
class EvolverInterface extends SimpleSwingApplication {
  val configPath = "./EvolverConfig.xml"
  var saveDirectory = "./data/"
  var numberOfLinesShown = 100
  var dataReady = false
  var dims = new Array[Int](2)
  var popSize = 20
  val reportArea = new TextArea
  reportArea.border_=(new TitledBorder(new LineBorder(java.awt.Color.black),"Program messages:")) //new SoftBevelBorder(BevelBorder.RAISED)
  reportArea.editable_=(false)
  val fitnessLabel = new Label("NaN") //
  var numThreads = 4
  var supervisor = new EvolutionSupervisor(reportArea,fitnessLabel,numThreads)
  var evolvers = List[NeuralEvolver]()
  //var evolvers2 = List[NeuralEvolver[Float]]()
  val rnd = new Random
  var printInfo = false
  val start = new Button {
	  text = "Start Evolution"
	  enabled_=(false)
  }
  val rCol = new Color(100,100,100)
  val sCol = new Color(200,200,200)
  var normalizeData = true
  object dataPanel extends TextArea { editable_=(false); border_=(new TitledBorder(new SoftBevelBorder(BevelBorder.LOWERED),"Data in memory:")) }
  object dataCounter extends Label { text_=("No data") }
  val iWorker = new InterfaceWorker(dataPanel,dataCounter,start)
  val dworker = new DataWorker(supervisor.getReporter,iWorker,normalizeData)
  //val modes = initModes
  var evolutionMode = 0 // 0 basic 2 evolino
  var initialDistScale = 0.1d
  var mutProb = 0.07
  var flipProb = 0.01
  var burstMutationFreq = 17
  var autoSave = 500
  var autoSaveBackup = 500
  var maxSteps = 10000L
  var actFun: Function1[Double,Double] = new OutFun
  var dimX = 540
  var dimY = 400
  //var defaultLearningMode = 0
  //var parFields = new Array[TextField](6)
  object mutProbField extends TextField(mutProb.toString) {
    object mutprobAction extends Action("") {
      def apply : Unit = {
        try {
          mutProb = mutProbField.text.toDouble
          reportArea.append("Changing the mutProb to "+mutProb+"\n")
        } catch {
          case _ => reportArea.append("Could not parse the value of mutProb.\n")
        }
      }
    }
    action_=(mutprobAction)
  }
  object flipProbField extends TextField(flipProb.toString) {
    object flipAction extends Action("") {
      def apply : Unit = {
        try {
          flipProb = flipProbField.text.toDouble
          reportArea.append("Changing the flipProb to "+flipProb+"\n")
        } catch {
          case _ => reportArea.append("Could not parse the value of flipProb.\n")
        }
      }
    }
    action_=(flipAction)
  }
  object scaleField extends TextField(initialDistScale.toString) {
    object scaleAction extends Action("") {
      def apply : Unit = {
        try {
          initialDistScale = scaleField.text.toDouble
          reportArea.append("Dist scale is now "+initialDistScale+"\n")
        } catch {
          case _ => reportArea.append("Could not parse the value of distScale.\n")
        }
      }
    }
    action_=(scaleAction)
  }
  object burstFreqField extends TextField(burstMutationFreq.toString) {
    object burstAction extends Action("") {
      def apply : Unit = {
        try {
          burstMutationFreq = burstFreqField.text.toInt
          reportArea.append("Burst Freq: "+burstMutationFreq+"\n")
        } catch {
          case _ => reportArea.append("Could not parse the typed value.\n")
        }
      }
    }
    action_=(burstAction)
  }
  object autoSaveField extends TextField(autoSave.toString) {
    object saveAction extends Action("") {
      def apply : Unit = {
        try {
          autoSave = autoSaveField.text.toInt
          reportArea.append("Autosaving every "+autoSave+" steps.\n")
        } catch {
          case _ => reportArea.append("Could not parse that value.\n")
        }
      }
    }
    action_=(saveAction)
  }
  var spawnFreq = 50
  object spawnFreqField extends TextField(spawnFreq.toString) {
    object freqAction extends Action("") {
      def apply : Unit = {
        try {
          spawnFreq = spawnFreqField.text.toInt
          reportArea.append("Changing the spawnFreq to "+spawnFreq+"\n")
        } catch {
          case _ => reportArea.append("Could not parse the value of spawnFreq.\n")
        }
      }
    }
    action_=(freqAction)
  }
  //val actFunField = new TextField(actFun.toString)
  object printInfoSelector extends CheckBox { 
    selected_=(printInfo)
    object printAction extends Action("Print Info") {
      def apply : Unit = {
        if (printInfoSelector.selected) {
          reportArea.append("Now printing info when evolving.\n")
          printInfo = true
        }
        supervisor.printInfo(printInfoSelector.selected)
      }
    }
    action_=(printAction)
  }
  object threadsField extends TextField(numThreads.toString()) {
    object threadsAction extends Action("") {
      def apply : Unit = {
        try {
          numThreads = threadsField.text.toInt
          reportArea.append("Number of concurrent Evolvers is now "+numThreads+"\n")
        } catch {
          case _ => reportArea.append("Could not parse that value.\n")
        }
      }
    }
    action_=(threadsAction)
  }
  //val defModeField = new TextField(defaultLearningMode.toString)
  object maxStepsField extends TextField(maxSteps.toString) {
    object stepsAction extends Action("") {
      def apply : Unit = {
        try {
          maxSteps = maxStepsField.text.toLong
          reportArea.append("Maximum number of steps is now "+maxSteps+"\n")
        } catch {
          case _ => reportArea.append("No luck parsing that value.\n")
        }
      }
    }
    action_=(stepsAction)
  }
  object saveDirSelector extends Button(saveDirectory) {
    object saveDirAction extends Action("") {
      def apply : Unit = {
        val dirChooser = new FileChooser
        dirChooser.fileSelectionMode_=(SelectionMode.DirectoriesOnly)
        val ok = dirChooser.showOpenDialog(saveDirSelector)
        if (ok.toString.equals("Approve")) {
          saveDirectory = dirChooser.selectedFile.toString
        }
        saveDirSelector.text_=(saveDirectory)
      }
    }
    action_=(saveDirAction)
  }
  object autoNormalize extends CheckBox {
    selected_=(normalizeData)
    object normalizationAction extends Action("AutoNormalize") {
      def apply : Unit = {
        normalizeData = autoNormalize.selected
        if (normalizeData) {
          reportArea.append("Autornomalizing loaded data.\n")
          dworker.normalizeData(true)
        }
        else {
          reportArea.append("Data will not be normalized automatically.\n")
          dworker.normalizeData(false)
        }
      }
    }
    action_=(normalizationAction)
  }
  //saveDirField.editable_=(true)
  val configurationReady = new Button("All Done")
  val functionChooser = new ComboBox(Seq[String]("Bipolar Sigmoid","Sigmoid"))
  var bRNN:RNND = null //EvoLauncher.createRNN
  var netReady = false
  val confWindow = new Frame { title_=("Configure Parameters") }
  val writeBestNet = new MenuItem("Save Best Network")
  writeBestNet.enabled_=(false)
  var schedule:CoolingSchedule = new SimpleSchedule(mutProb,flipProb,maxSteps)
  val scheduleChooser = new ComboBox(Seq[String]("SimpleSchedule","AdaptiveSchedule"))
  val modes = Seq[String]("Basic ESP","ESP+","Evolino")
  val modeSelector = new ComboBox(modes)
  val repopulatorSelector = new ComboBox(Seq[String]("BasicRP","ComplexRP"))
  var startedBefore = false
  
  val popSizeSlider = new Slider {
	max_=(100)
	min_=(5)
	value_=(popSize)
  }
  
  def readConfig : Unit = {
    val cFile = new File(configPath)
    if (cFile.exists) {
      val e = XML.loadFile(cFile)
      try {
        dimX = (e \\ "DimensionX").text.toInt
      } catch {
        case _ => reportArea.append("Could not read DimensionX from config.\n")
      }
      try {
        dimY = (e \\ "DimensionY").text.toInt
      } catch {
        case _ => reportArea.append("Could not read DimensionY from config.\n")
      }
      try {
        numThreads = (e \\ "NumThreads").text.toInt
        threadsField.text_=(numThreads.toString)
      } catch {
        case _ => reportArea.append("Could not read numThreads from config.\n")
      }
      try {
        initialDistScale = (e \\ "InitialDistScale").text.toDouble
        scaleField.text_=(initialDistScale.toString)
      } catch {
        case _ => reportArea.append("Could not read InitialDistScala from config.\n")
      }
      //
      //popSize = (e \\ "PopSize").text.toInt
      try {
        mutProb = (e \\ "MutProb").text.toDouble
        mutProbField.text_=(mutProb.toString)
      } catch {
        case _ => reportArea.append("Could not read MutProb from config.\n")
      }
      try {
        flipProb = (e \\ "FlipProb").text.toDouble
        flipProbField.text_=(flipProb.toString)
      } catch {
        case _ => reportArea.append("Could not read flipProb from config.\n")
      }
      try {
        burstMutationFreq = (e \\ "BurstMutationFreq").text.toInt
        burstFreqField.text_=(burstMutationFreq.toString)
      } catch {
        case _ => reportArea.append("Could not read the frequency of burst mutations.\n")
      }
      try {
        autoSave = (e \\ "AutoSave").text.toInt
      } catch {
        case _ => reportArea.append("Using default autoSave frequency.\n")
      }
      try {
        val afun:String = (e \\ "ActFun").text
        afun match {
          case "SigmoidExp" => actFun = new SigmoidExp; functionChooser.item_=("SigmoidExp")
          case "BiSigmoid" => actFun = new OutFun; functionChooser.item_=("BiSigmoidExp")
          case _ => actFun = new SigmoidExp
        }
      } catch {
        case _ => reportArea.append("Could not read which Activation Function to use.\n")
      }
      try {
        saveDirectory = (e \\ "SaveDir").text
        saveDirSelector.text_=(saveDirectory)
      } catch {
        case _ => reportArea.append("Could not find saveDirectory.\n")
      }
      try { 
        maxSteps = (e \\ "MaxSteps").text.toLong
        maxStepsField.text_=(maxSteps.toString)
      } catch {
        case _ => reportArea.append("Could not read the value of MaxSteps from config.\n")
      }
      try {
        val srep = (e \\ "CSchedule").text
        srep match {
          case "SimpleSchedule" => schedule = new SimpleSchedule(mutProb,flipProb,maxSteps); scheduleChooser.item_=("SimpleSchedule")
          case "AdaptiveSchedule" => schedule = new AdaptiveSchedule(mutProb,flipProb,maxSteps); scheduleChooser.item_=("AdaptiveSchedule")
          case _ => reportArea.append("Could not determine what Cooling Schedule to use.\n")
        }
      } catch {
        case _ => reportArea.append("Could not determine what Cooling Schedule to use.\n")
      }
      try {
        printInfo = (e \\ "PrintInfo").text.toBoolean
        if (printInfo) {
          printInfoSelector.selected_=(true)
        }
      } catch {
        case _ => reportArea.append("Could not determine whether to print info all the time or not.\n")
      }
      try {
        val lMode = (e \\ "LearningMode").text
        lMode match {
          case "Basic ESP" => modeSelector.selection.index_=(0)
          case "ESP+" => modeSelector.selection.index_=(1)
          case "Evolino" => modeSelector.selection.index_=(2)
          case _ => modeSelector.selection.index_=(0)
        }
      } catch {
        case _ => reportArea.append("Using default learning mode.\n")
      }
      try {
        popSize = (e \\ "SubpopulationSize").text.toInt
        popSizeSlider.value_=(popSize)
      } catch {
        case _ => reportArea.append("Using default subpopulation size.\n")
      }
      try {
        normalizeData = (e \\ "AutoNormalize").text.toBoolean
      } catch {
        case _ => reportArea.append("Could not find the value of AutoNormalize.\n")
      }
      try {
        spawnFreq = (e \\ "SpawnFreq").text.toInt
      } catch {
        case _ => reportArea.append("Could not read the value of SpawnFreq.\n")
      }
    }
    else {
      reportArea.append("Could not read the config file.\n")
      reportArea.append("You can save your config after setting the parameters.\n")
    }
  }
  def writeConfig : Boolean = {
    val e1 = new Array[Elem](18)
    e1(0) = <DimensionX>{dimX}</DimensionX>
    e1(1) = <DimensionY>{dimY}</DimensionY>
    e1(10) = <NumThreads>{numThreads}</NumThreads>
    e1(2) = <InitialDistScale>{initialDistScale}</InitialDistScale>
    e1(3) = <AutoSave>{autoSave}</AutoSave>
    e1(4) = <PopSize>{popSize}</PopSize>
    e1(5) = <MutProb>{mutProb}</MutProb>
    e1(6) = <FlipProb>{flipProb}</FlipProb>
    e1(7) = <BurstMutationFreq>{burstMutationFreq}</BurstMutationFreq>
    e1(8) = <ActFun>{functionChooser.selection.item}</ActFun>
    e1(9) = <PrintInfo>{printInfo}</PrintInfo>
    e1(11) = <LearningMode>{modeSelector.selection.item}</LearningMode>
    e1(12) = <MaxSteps>{maxSteps}</MaxSteps>
    e1(13) = <SaveDir>{saveDirectory}</SaveDir>
    e1(14) = <CSchedule>{scheduleChooser.selection.item}</CSchedule>
    e1(15) = <SubpopulationSize>{popSize}</SubpopulationSize>
    e1(16) = <AutoNormalize>{normalizeData}</AutoNormalize>
    e1(17) = <SpawnFreq>{spawnFreq}</SpawnFreq>
    //e1(15) =
    val xrep = <EvolverConfig>{for (i <- 0 until e1.length) yield e1(i)}</EvolverConfig>
    val f = new File(configPath)
    if (f.exists) {
      f.delete
      reportArea.append("Replacing the old config file with the new configuration.\n")
      XML.save(configPath,xrep,"UTF-8")
    }
    else {
      //val fw = new FileWriter(f)
      XML.save(configPath,xrep,"UTF-8")
      reportArea.append("Saving the configuration...\n")
    }
    return true
  }
  def configure : Unit = {
    //autoSaveField.text_=(autoSave.toString)
    //threadsField.text_=(numThreads.toString)
    val cnfPanel = new GridPanel(7,4) {
      contents += new Label("Threads:")
      contents += threadsField

      contents += new Label("Dist Scale:")
      contents += scaleField
      contents += new Label("Save Freq")
      contents += autoSaveField
      contents += new Label("Mut prob:")
      contents += mutProbField
      contents += new Label("Flip prob:")
      contents += flipProbField
      contents += new Label("Burst Freq")
      contents += burstFreqField
      contents += new Label("Act Fun:")
      contents += functionChooser//actFunField
      contents += new Label("Max Steps:")
      contents += maxStepsField
      contents += new Label("Cooling Schedule:")
      contents += scheduleChooser
      contents += new Label("Repopulator:")
      contents += repopulatorSelector
      contents += new Label("SpawnFreq:")
      contents += spawnFreqField//new Label("")
      contents += printInfoSelector
      contents += autoNormalize
      contents += new Label("SaveDir:")
      contents += saveDirSelector
      contents += configurationReady
    }
    cnfPanel.border_=(Swing.EtchedBorder(Swing.Raised,rCol,sCol))
    confWindow.contents_=(cnfPanel)
    
    confWindow.pack
    confWindow.visible_=(true)
  }
  def initPopulation() : Unit = {
    dims = dworker.getDims
    popSize = popSizeSlider.value
    val allPops = new Array[CellPopulationD](numThreads)
    val allNets = new Array[NetPopulationD](numThreads)
    val allEvolvers = new Array[NeuralEvolver](numThreads)
    val rnd = new Random
    printInfo = printInfoSelector.selected
    supervisor.setPrintInfo(printInfo)
    val scheduleRep = scheduleChooser.selection.item
    var schedule:CoolingSchedule = new SimpleSchedule(mutProb,flipProb,maxSteps)
    var populator1:Repopulator[CellPopulationD] = new BasicRepopulator
    var populator2:NetRepopulator[NetPopulationD,CellPopulationD] = new SimpleNetRepopulator
    val populatorRep = repopulatorSelector.selection.item
    populatorRep match {
        case "ComplexRP" => populator1 = new ComplexRepopulator(0.75); populator2 = new VariableNetRepopulator(0.8)
        case _ => Unit
    }
    scheduleRep match {
      case "AdaptiveSchedule" => schedule = new AdaptiveSchedule(mutProb,flipProb,maxSteps)
      case _ => Unit
    }
    for (i <- 0 until numThreads) {
      allPops(i) = new CellPopulationD(dims(0),if (i % 2 == 0) 1 else 2,dims(1),popSize)
      allPops(i).init(initialDistScale,1,rnd)
      allNets(i) = new NetPopulationD(allPops(i))
      allNets(i).init
      allEvolvers(i) = new NeuralEvolver(allPops(i),allNets(i),supervisor,1,supervisor.getReporter,rnd)
      allEvolvers(i).addDLists(dworker.getDLists)
      allEvolvers(i).setActFun(actFun)
      allEvolvers(i).setBurstFreq(burstMutationFreq)
      allEvolvers(i).setPrintInfo(printInfo)
      
      allEvolvers(i).setEvoMode(modes.indexOf(modeSelector.selection.item))
      allEvolvers(i).setRepopulator(populator1)
      allEvolvers(i).setNetRepopulator(populator2)
      allEvolvers(i).setSpawningFreq(spawnFreq)
      allEvolvers(i).setSchedule(schedule)

      
      /*
      if (evolutionMode == 2) {
        allEvolvers(i).addData2(dworker.getAsList(2),dworker.getAsList(3))
      }
      */
      //allEvolvers(i).setMutationProb(mutProb)
      //allEvolvers(i).setFlipProb(flipProb)
      reportArea.append(allEvolvers(i).getSimpleRepresentation+"\n")
      allEvolvers(i).start
      supervisor.addEvolver(i,allEvolvers(i))
    }
    supervisor.setThreads(numThreads)
	supervisor.start
	supervisor ! "Start" //UpdateNow(0L)
	writeBestNet.enabled_=(true)
  }
  def init : Unit = {
    val frame = new MainFrame()
	frame.pack
	frame.open
	}
  def top = new MainFrame {
	title = "NeuroGenesisInterface"
	object fChooser extends FileChooser { multiSelectionEnabled_=(true) }
	object openFiles extends Button { text_=("Select Data") }
	readConfig //
	val sizeField = new TextField("Subpopulation size: "+popSizeSlider.value)
	sizeField.editable_=(false)
	
	contents = new GridPanel(1,2) {
	  val lpane1 = new GridPanel(7,1) {
	    contents += openFiles
	    contents += dataCounter
	    contents += sizeField
	    contents += popSizeSlider
	    contents += modeSelector
	    contents += start
	    contents += fitnessLabel
	    border = Swing.EtchedBorder(Swing.Raised,rCol,sCol)
	  }
	  val lpane2 = new SplitPane(Orientation.Horizontal,lpane1,new ScrollPane(dataPanel))
	  lpane2 //.border_=(Swing.EtchedBorder)
	  val rpane = new GridPanel(1,1) {
	    contents += new ScrollPane(reportArea)
	  }
	  val splitPane = new SplitPane(Orientation.Vertical,lpane2,rpane)
	  splitPane.border_=(Swing.EtchedBorder)
	  contents += splitPane
	}
	val stopEvolution = new MenuItem("Stop!") { enabled_=(false) }
	val plotData = new MenuItem("Plot Data") { enabled_=(false) }
	val runLeastSquares = new MenuItem("Linear Regression") { enabled_=(false) }
	val makePredictions = new MenuItem("Predict using best network") { enabled_=(false) }
	val calculateValidationError = new MenuItem("Validate best solution") { enabled_=(false)}
	val writeConfigNow = new MenuItem("Write Config")
	val configureNow = new MenuItem("Configure")

	val readMatrix = new MenuItem("Read Matrix")
	val readRNN = new MenuItem("Read RNN")
	val autoSaveButton = new CheckMenuItem("AutoSave")
	val displayNet = new MenuItem("Display Best RNN") { enabled_=(false) }
	val layoutSelector = new ComboBox(Seq[String]("FR","KK","ISOM","Spring"))
	val layoutOK = new Button("Draw!")
	val clearReportArea = new MenuItem("Clear Messages")
	val clearData = new MenuItem("Clear data!") { enabled_=(false) }
	object gogo extends MenuItem("Go supervisor, go!") { 
	  object goAction extends Action("Restart Supervisor!") {
	    def apply : Unit = {
	      supervisor ! UpdateNow(0)
	    }
	  }
	  action_=(goAction)
	}
	val writeBestPopulation = new MenuItem("Write Best Population!") { enabled_=(false) }
	val lFrame = new Frame // used when selecting the layout of the net that will be displayed
	if (autoSave == 0) {
	  autoSaveButton.selected_=(false)
	}
	else {
	  autoSaveButton.selected_=(true)
	}
	val mBar = new MenuBar() {
	  contents += new Menu("File") {
	    
	    contents += readMatrix
	    contents += readRNN
	    contents += new Separator
	    contents += writeBestNet
	    contents += writeConfigNow
	    contents += new Separator
	    contents += clearData
	  }
	  contents += new Menu("Util") {
	    
	    
	    //contents += new Separator
	    contents += clearReportArea
	    contents += new Separator
	    contents += displayNet
	    contents += plotData
	    contents += makePredictions
	    contents += new Separator
	    
	    contents += new Separator
	    contents += runLeastSquares
	    contents += new Separator
	    contents += calculateValidationError
	    
	  }
	  contents += new Menu("Config") {
	    contents += configureNow
	  }
	  contents += new Menu("Control") {
	    contents += gogo //supervisor seems to halt sometimes.. this tells it to go on again
	    contents += new Separator
	    contents += stopEvolution
	  }
	  //name_=()
	}
	menuBar_=(mBar)
	minimumSize_=(new Dimension(dimX,dimY))
	listenTo(openFiles,start,stopEvolution,popSizeSlider,plotData,
	    configureNow,writeConfigNow,readMatrix,readRNN,
	    configurationReady,functionChooser,calculateValidationError,
	    writeBestNet,displayNet,modeSelector,scheduleChooser,runLeastSquares,clearReportArea,
	    makePredictions,clearData,repopulatorSelector,writeBestPopulation)
	//listenTo(configurationReady,saveDirField,maxStepsField,functionChooser)
	//listenTo(writeBestNet,displayNet,modeSelector)
	var nclicks = 0
	dworker.start()
	iWorker.start
	reactions += {
	  case ButtonClicked(`openFiles`) => {
	    val reval = fChooser.showOpenDialog(contents.first)
	    if (reval.toString.equals("Approve")) {
	      //dworker.readDoubles(fChooser.selectedFile)
	      dworker ! LoadData(fChooser.selectedFiles)
	      plotData.enabled_=(true)
	      clearData.enabled_=(true)
	    }
	  }
	  case ButtonClicked(`readMatrix`) => {
	    val reval = fChooser.showOpenDialog(contents.first)
	    if (reval.toString.equals("Approve")) {
	      dworker.readMatrix(fChooser.selectedFile)
	    }
	  }
	  case ButtonClicked(`start`) => {
	    if (modeSelector.selection.item == "Evolino" && dworker.getCount < 4) {
	      reportArea.append("Evolino requires 4 data arrays.\n")
	    }
	    else {
	    if (!startedBefore) {
	      initPopulation
	      displayNet.enabled_=(true)
	      writeBestNet.enabled_=(true)
	      runLeastSquares.enabled_=(true)
	      startedBefore = true
	      start.enabled_=(false)
	      stopEvolution.enabled_=(true)
	      if (dworker.getCount > 4 || (modes.indexOf(modeSelector.selection.item) < 2 && dworker.getCount > 2)) {
	        makePredictions.enabled_=(true)
	      }
	    }
	    else {
	      supervisor = new EvolutionSupervisor(reportArea,fitnessLabel,numThreads)
	      initPopulation
	      reportArea.append("Started with a fresh population.\n")
	      start.enabled_=(false)
	      writeBestPopulation.enabled_=(false)
	    }
	    }
	  }
	  case ButtonClicked(`runLeastSquares`) => {
	    val bestRNN = supervisor.getSuperStar
	    bestRNN.reset
	    val trData1 = bestRNN.evolinoFeed(dworker.getData(0),actFun)
	    var trData2 = bestRNN.evolinoFeed(dworker.getData(2),actFun)
	    NeuralOps.runLinearRegression(trData1,dworker.getData(1),trData2,dworker.getData(3),reportArea)
	    val rnnRep = bestRNN.toXML
	    prettyPrint(rnnRep)
	  }
	  case ValueChanged(`popSizeSlider`) => {
	    sizeField.text_=("Subpopulation size: "+popSizeSlider.value)
	  }
	  case ButtonClicked(`stopEvolution`) => {
	    supervisor ! "Exit"
	    start.text_=("Start Again!")
	    start.enabled_=(true)
	    calculateValidationError.enabled_=(true)
	    writeBestPopulation.enabled_=(true)
	  }
	  case ButtonClicked(`clearData`) => {
	    dworker.removeAllData
	    clearData.enabled_=(false)
	    start.enabled_=(false)
	    runLeastSquares.enabled_=(false)
	    calculateValidationError.enabled_=(false)
	    makePredictions.enabled_=(false)
	    dataCounter.text_=("Data arrays: 0")
	    iWorker.reset
	    dataPanel.text_=("")
	  }
	  case ButtonClicked(`calculateValidationError`) => {
	    val bestRNN = supervisor.getSuperStar.makeClone
	    bestRNN.feedData(dworker.getData(0),actFun)
	    val res1 = bestRNN.feedData(dworker.getData(2),actFun)
	    reportArea.append("Validation error was: "+NeuralOps.totalError(dworker.getData(3),res1.toList)+"\n")
	  }
	  case ButtonClicked(`makePredictions`) => {
	    if (modes.indexOf(modeSelector.selection.item) < 2) {
	      predict(2)
	    }
	    else {
	      predict(4)
	    }
	  }
	  /*
	  case ButtonClicked(`autoSaveButton`) => {
	    if (autoSave == 0) {
	      autoSave = autoSaveBackup
	    }
	    else {
	      autoSaveBackup = autoSave
	      autoSave = 0
	    }
	  }
	  */
	  case ButtonClicked(`clearReportArea`) => {
	    reportArea.text_=("All clear!\n")
	  }
	  case ButtonClicked(`writeBestNet`) => {
	    writeSuperStar(new File(saveDirectory+"superstar.txt"))
	  }
	  case ButtonClicked(`displayNet`) => {
	    //displayBestNet using the Jung library but first creates a window used to choose the layout algorithm
	    val loPane = new GridPanel(2,1) {
	      contents += layoutSelector
	      contents += layoutOK
	    }
	    listenTo(layoutOK,layoutSelector)
	    lFrame.contents_=(loPane)
	    lFrame.minimumSize_=(new Dimension(200,100))
	    lFrame.pack
	    lFrame.visible_=(true)
	  }
	  case ButtonClicked(`layoutOK`) => {
	    val rnn = supervisor.getSuperStar
	    val gWorker = new GraphWorker(layoutSelector.selection.item)
	    gWorker.start
	    gWorker ! AnotherRNN(rnn)
	    lFrame.close()
	  }
	  case SelectionChanged(`modeSelector`) => {
	    val s = modeSelector.selection.item
	    s match {
	      case "Basic ESP" => evolutionMode = 0; 
	      case "ESP+" => evolutionMode = 1
	      case "Evolino" => evolutionMode = 2
	      case _ => evolutionMode = 0
	    }
	    reportArea.append("Selected mode: "+s+" ("+evolutionMode+")\n")
	  }
	  case SelectionChanged(`functionChooser`) => {
	    val s = functionChooser.selection.item
	    s match {
	      case "BiSigmoid" => actFun = new OutFun; reportArea.append("Changed actFun to: "+actFun.toString+"\n")
	      case "Sigmoid" => actFun = new SigmoidExp; reportArea.append("Changed actFun to: "+actFun.toString+"\n")
	      case _ => actFun = new SigmoidExp; reportArea.append("Changed actFun to: "+actFun.toString+"\n")
	    }
	  }
	  case SelectionChanged(`scheduleChooser`) => {
	    val s = functionChooser.selection.item
	    s match {
	      case "SimpleSchedule" => schedule = new SimpleSchedule(mutProb,flipProb,maxSteps)
	      case "AdaptiveSchedule" => schedule = new AdaptiveSchedule(mutProb,flipProb,maxSteps)
	      case _ => reportArea.append("Error changing the Cooling Schedule.\n")
	    }
	  }
	  case ButtonClicked(`plotData`) => {
	    //dworker.plotColumns
	    dworker.makeSubplots
	  }
	  case ButtonClicked(`writeConfigNow`) => {
	    writeConfig
	  }
	  case ButtonClicked(`configurationReady`) => {
	    confWindow.close()
	  }
	  case ButtonClicked(`configureNow`) => {
	    configure
	  }
	  /*
	  case EditDone(`threadsField`) => {
	    numThreads = threadsField.text.toInt
	    reportArea.append("Number of threads to use is now: "+numThreads+"\n")
	  }
	  case EditDone(`mutProbField`) => {
	    mutProb = mutProbField.text.toDouble
	    reportArea.append("Mutation probability at start is now: "+mutProb.toString+"\n")
	  }
	  case EditDone(`flipProbField`) => {
	    flipProb = flipProbField.text.toDouble
	    reportArea.append("The probability to flip whether connections are expressed is now: "+flipProb+"\n")
	  }
	  case EditDone(`burstFreqField`) => {
	    burstMutationFreq = burstFreqField.text.toInt
	    reportArea.append("Performing burst mutations every "+burstMutationFreq+" steps if there's no improvement otherwise.\n")
	  }
	  case EditDone(`autoSaveField`) => {
	    autoSave = autoSaveField.text.toInt
	    if (autoSave < 0) {
	      autoSaveField.text_=("0")
	      autoSave = 0
	    }
	  }
	  case EditDone(`maxStepsField`) => {
	    maxSteps = maxStepsField.text.toLong
	    reportArea.append("Changed Max Steps to: "+maxSteps+"\n")
	  }
	  case EditDone(`scaleField`) => {
	    initialDistScale = scaleField.text.toDouble
	  }
	  case EditDone(`defModeField`) => {
	    defaultLearningMode = defModeField.text.toInt
	    
	  }
	  */
	  case ButtonClicked(`readRNN`) => {
	    reportArea.append("Trying to read a RNN.\n")
	    val reval = fChooser.showOpenDialog(contents.first)
	    if (reval.toString.equals("Approve")) {
	      val xrep = readElem(fChooser.selectedFile)
	      /*
	      val stringRep = readXML(fChooser.selectedFile).substring(38)//<?xml version='1.0' encoding='UTF-8'?>
	      reportArea.append(stringRep)
	      val xmlRep = XML.loadString(stringRep)
	      reportArea.append("\n")
	      reportArea.append(xmlRep.toString)
	      */
	      /*
	      val xrep = readElem(fChooser.selectedFile)
	      println("Size: "+xrep.size)
	      val q = XMLOperator.customFilter(xrep,"NeuralConnsD")
	      for (q0 <- q) {
	        reportArea.append(q0.toString+"\n")
	      }
	      reportArea.append("the representation is: "+xrep+"\n")
	      //bRNN.fromXML(xrep)
	       * 
	       */
	    }
	    else {
	      reportArea.append("Cancelled the procedure for loading an xml representation of a neural net.\n")
	    }
	  }
	}
  }

  def predict(idx:Int) : Unit = {
    val rnn = if (!netReady) supervisor.getSuperStar else bRNN
    var j = 0
    var res = new Array[Array[Double]](0)//
    rnn.reset
    val resArea = new TextArea
    while (j <= idx) {
      val a1 = dworker.getData(j) //
      res = rnn.feedData(a1,actFun)
      resArea.append("Output for input: "+j+"\n")
      for (k <- 0 until res.length) {
        for (m <- 0 until res(k).length) {
          resArea.append(res(k)(m).toString+" ")
        }
        resArea.append("\n")
      }
      j += 2
    }
    val rnnRep = rnn.toString
    val rnnDescription = new TextArea(rnnRep) {
      border_=(new TitledBorder(new LineBorder(java.awt.Color.black),"The best RNN:"))
    }
    object drawPredictions extends MenuItem("Plot Predictions") {
      object plotAction extends Action("Plot Predictions") {
        def apply : Unit = {
          for (i <- 0 until res(0).length) {
            val (x,y) = dworker.getAsTensors(res,i)
            Plotting.subplot(res(0).length,1,i+1)
            Plotting.plot(x,y)
          }
        }
      }
      action_=(plotAction)
    }
    val predictionsPane = new GridPanel(2,1) {
      /*
      val panelX = new GridPanel(2,1) {
        contents += new ScrollPane(rnnDescription)
        contents += drawPredictions
      }
      */
      contents += new ScrollPane(resArea)
      contents += new ScrollPane(rnnDescription)//panelX
    }

    val resFrame = new Frame {
      title_=("PredictionsFrame")
      contents_=(predictionsPane)
      val predMenu = new Menu("Predictions") {
        contents += drawPredictions
      }
      val predMBar = new MenuBar {
        contents += predMenu
      }
      menuBar_=(predMBar)
    }
    resFrame.pack
    resFrame.visible_=(true)
    
  }
  def createNullRNN : RNND = {
    val il = new Array[InCellD](2)
    val bl = new Array[CellBlockD](2)
    val ol = new Array[OutCellD](2)
    new RNND(il,bl,ol)
  }
  def readElem(f:File) : Elem = {
    val e = XML.load(Source.fromFile(f))
    println(e.toString)
    e
  }
  def appendText(f:File) : Unit = {
    val br = new BufferedReader(new FileReader(f))
	while (br.ready) {
	  reportArea.append(br.readLine+"\n")
	}
	br.close
  }
  def prettyPrint(e:Elem) : Unit = {
    val s = e.toString
    val ls = 50
    var idx = 0
    var idx2 = ls
    val l = s.length
    while (idx2 < l) {
      reportArea.append(s.substring(idx,idx2)+"\n")
      idx = idx2
      idx2 += ls
    }
    reportArea.append(s.substring(idx,l))
  }
  def readXML(f:File) : String = {
    val sb = new StringBuilder
    val br = new BufferedReader(new FileReader(f))
    while (br.ready) {
      sb.append(br.readLine)
    }
    br.close
    sb.toString
  }
  /*Writes the best network to a file f in a format provided by JUNG
   *and not in the more natural XML format for RNND
   */
  def writeSuperStar(f:File) : Unit = {
    val sDir = new File(saveDirectory)
    if (!sDir.exists()) {
      sDir.mkdir()
    }
    val bestNet = supervisor.getSuperStar
    val graphRep = bestNet.toGraph
    val writer = new FileWriter(f)
    val gWriter = new GraphMLWriter[Int,String]
    gWriter.save(graphRep,writer)//
    writer.close()
  }
  def graph2Img(g:SparseGraph[Int,String]) : Image = {
    val lOut = new FRLayout(g)
    lOut.initialize()
    val imgServer = new VisualizationImageServer(lOut,new Dimension(640,480))
    imgServer.getImage(new Point(370,240),new Dimension(640,480))
  }
  def displayBestNet : Unit = {
    val bestNet = supervisor.getSuperStar
    val graphRep = bestNet.toGraph
    val img = graph2Img(graphRep)
    //ImageIO.write(img,"jpg",new File(saveDirectory+"bestNet.jpg"))
    val displayWindow = new JFrame
    val displayPanel = new DisplayPanel(img)
    displayPanel.setSize(640,480)
    //val g = img.getGraphics()
    displayWindow.setContentPane(displayPanel)
    //g.drawImage(img,0,0,displayPanel)
    displayWindow.pack()
    displayWindow.setVisible(true)
  }

}