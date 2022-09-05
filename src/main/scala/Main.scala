import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import com.applaudostudios.fcastro.HPProject.Actors.{JsonLoaderActor, StoreActor}
import com.applaudostudios.fcastro.HPProject.Router

object Main {

 /* @tailrec
  private def commandLoop(loader:ActorRef): Unit = {
    print("Introduce filename for .json file to load:")
    StdIn.readLine() match {
      case file:String if Files.exists(Paths.get(file)) =>
        println(s"Loading file $file")
        loader ! JsonLoaderActor.LoadFile(Paths.get(file))
        commandLoop(loader)
      case "quit" | "Quit" | "q" => println("Loading over")
      case s:String =>
        println(s"$s not found!")
        commandLoop(loader)
    }
  }*/

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("MyAkkaSystem")
    val store = system.actorOf(Props(new StoreActor()),"StoreActor")
    val loader:ActorRef = system.actorOf(JsonLoaderActor.props(store))

    val router = Router(store,loader);
    val source=Http().newServerAt("localhost",8080)
    source.bind(router.routes)
    //commandLoop(loader)
  }
}