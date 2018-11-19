package cn.piflow.bundle.memcache

import cn.piflow.{JobContext, JobInputStream, JobOutputStream, ProcessContext}
import cn.piflow.conf.{ConfigurableStop, PortEnum, StopGroupEnum}
import cn.piflow.conf.bean.PropertyDescriptor
import cn.piflow.conf.util.{ImageUtil, MapUtil}
import com.danga.MemCached.{MemCachedClient, SockIOPool}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

class PutMemcache extends ConfigurableStop{
  override val authorEmail: String = "yangqidong@cnic.cn"
  override val description: String = "get data from mongodb"
  val inportList: List[String] = List(PortEnum.DefaultPort.toString)
  val outportList: List[String] = List(PortEnum.NonePort.toString)

  var servers:String=_            //服务器地址和端口号Server address and port number,If you have multiple servers, use "," segmentation.
  var keyFile:String=_            //你想用来作为key的字段You want to be used as a field for key.
  var weights:String=_            //每台服务器的权重Weight of each server
  var maxIdle:String=_            //最大处理时间Maximum processing time
  var maintSleep:String=_         //主线程睡眠时间Main thread sleep time
  var nagle:String=_              //socket参数，若为true，则写数据时不缓冲立即发送If the socket parameter is true, the data is not buffered and sent immediately.
  var socketTO:String=_           //socket阻塞时候的超时时间Socket timeout during blocking
  var socketConnectTO:String=_    //连接建立时的超时控制Timeout control during connection establishment


  override def perform(in: JobInputStream, out: JobOutputStream, pec: JobContext): Unit = {

    val session: SparkSession = pec.get[SparkSession]()
    val inDF: DataFrame = in.read()

    //获取连接池实例对象
    val pool: SockIOPool = SockIOPool.getInstance()
    //    链接到数据库
    var serversArr:Array[String]=servers.split(",")
    pool.setServers(serversArr)

    if(weights.length>0){
      val weightsArr: Array[Integer] = "3".split(",").map(x=>{new Integer(x.toInt)})
      pool.setWeights(weightsArr)
    }
    if(maxIdle.length>0){
      pool.setMaxIdle(maxIdle.toInt)
    }
    if(maintSleep.length>0){
      pool.setMaintSleep(maintSleep.toInt)
    }
    if(nagle.length>0){
      pool.setNagle(nagle.toBoolean)
    }
    if(socketTO.length>0){
      pool.setSocketTO(socketTO.toInt)
    }
    if(socketConnectTO.length>0){
      pool.setSocketConnectTO(socketConnectTO.toInt)
    }

    pool.initialize()
    //建立全局唯一实例
    val mcc: MemCachedClient = new MemCachedClient()

    val fileNames: Array[String] = inDF.columns

    val rows: Array[Row] = inDF.collect()
    for(row <- rows){
      val rowStr: String = row.toString()
      val rowArr: Array[String] = rowStr.substring(1,rowStr.length-2).split(",")

      var map:Map[String,String]=Map()
      var key: String =""
      for(x <- (0 until fileNames.size)){
        var name: String = fileNames(x)
        val file = rowArr(x)
        if( ! name.equals(keyFile)){
          map+=(name -> file)
        }else{
          key = file
        }
      }
      mcc.set(key,map)
    }

  }

  override def setProperties(map: Map[String, Any]): Unit = {
    servers = MapUtil.get(map,"servers").asInstanceOf[String]
    keyFile = MapUtil.get(map,"keyFile").asInstanceOf[String]
    weights = MapUtil.get(map,"weights").asInstanceOf[String]
    maxIdle = MapUtil.get(map,"maxIdle").asInstanceOf[String]
    maintSleep = MapUtil.get(map,"maintSleep").asInstanceOf[String]
    nagle = MapUtil.get(map,"nagle").asInstanceOf[String]
    socketTO = MapUtil.get(map,"socketTO").asInstanceOf[String]
    socketConnectTO = MapUtil.get(map,"socketConnectTO").asInstanceOf[String]
  }

  override def getPropertyDescriptor(): List[PropertyDescriptor] = {
    var descriptor : List[PropertyDescriptor] = List()

    val servers=new PropertyDescriptor().name("servers").displayName("servers").description("Server address and port number,If you have multiple servers, use , segmentation.").defaultValue("").required(true)
    descriptor = servers :: descriptor
    val keyFile=new PropertyDescriptor().name("keyFile").displayName("keyFile").description("You want to be used as a field for key.").defaultValue("").required(true)
    descriptor = keyFile :: descriptor
    val weights=new PropertyDescriptor().name("weights").displayName("weights").description("Weight of each server,If you have multiple servers, use , segmentation.").defaultValue("").required(false)
    descriptor = weights :: descriptor
    val maxIdle=new PropertyDescriptor().name("maxIdle").displayName("maxIdle").description("Maximum processing time").defaultValue("").required(false)
    descriptor = maxIdle :: descriptor
    val maintSleep=new PropertyDescriptor().name("maintSleep").displayName("maintSleep").description("Main thread sleep time").defaultValue("").required(false)
    descriptor = maintSleep :: descriptor
    val nagle=new PropertyDescriptor().name("nagle").displayName("nagle").description("If the socket parameter is true, the data is not buffered and sent immediately.").defaultValue("").required(false)
    descriptor = nagle :: descriptor
    val socketTO=new PropertyDescriptor().name("socketTO").displayName("socketTO").description("Socket timeout during blocking").defaultValue("").required(false)
    descriptor = socketTO :: descriptor
    val socketConnectTO=new PropertyDescriptor().name("socketConnectTO").displayName("socketConnectTO").description("Timeout control during connection establishment").defaultValue("").required(false)
    descriptor = socketConnectTO :: descriptor

    descriptor
  }

  override def getIcon(): Array[Byte] = {
    ImageUtil.getImage("memcache/Memcache.png")
  }

  override def getGroup(): List[String] = {
    List(StopGroupEnum.Memcache.toString)
  }

  override def initialize(ctx: ProcessContext): Unit = { }

}