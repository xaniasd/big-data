#My experience and further notes

As someone making his first steps in the data engineering world, I found the assignment very interesting. It exposed me to lots of relevant tools in a hands-on way,
as well as introduced me to basic tasks in a data engineering context. It is clear that I just scraped the surface of it all, but certainly made a good starting point 
with this assignment.  

## HDP Sandbox

I have tried lots of self-contained educational setups in other domains, but rarely do they come without some fighting upfront. In that sense, I was pleasantly 
surprised by the HDP Sandbox, in the sense that it worked relatively easily from the start, and is organized in such a way that the learner can use all
available tools and tutorials without a lot of hassle. 

I found the documentation and Ambari quite intuitive and had little trouble finding my way and managing the services of HDP.

Minor negative points are the relatively large vm/container image (10Gb), which makes a good internet connection more than necessary.

I started off with the VM but quickly switched to the docker version. The VM is not as efficient in making use of computing resources and running that alongside 
a resource-intensive IDE such as IntelliJ didn't make my laptop happy.
 
I tried deploying the VM on a headless server, only to realize that the pre-configured port forwarding rules on VirtualBox bind only to localhost. As a result, I couldn't work
with the server remotely without changing 70 rules one by one at the command line. The docker version didn't suffer from such issues.

In short, the docker version of the sandbox is recommended for a better developer experience.


## HDFS & Hive on Spark

I decided to implement the solution in scala, just to get my hands dirty on this language as well. Starting with SBT didn't prove to be a good choice though,
as I didn't manage to get the code compiled after quite some struggling. Certainly a part of it has to do with lack of understanding from my side; on the other hand
there seem to be issues with versions of dependent packages and plugins that should work (this is not a complex setup after all). On top of that, I managed to get 
it all working pretty fast when I switched to Gradle, and even do slightly fancier stuff such as building two different jar files. 

Needless to say that I didn't invent any wheels during this assignment, but got inspiration by internet sources and books. It couldn't have gone any other way 
at this stage as there's a lot of new concepts to grasp in a relatively short time. 

The `DriverApp` object implements the solution, except for uploading the input files to HDFS. I did this manually but should be trivial to automate with a script 
(using `scp` to copy the files to the sandbox and `hadoop fs put` to load them to HDFS). Another option would be to download the file locally from a URI
and save it with scala code but I found it not really exciting to implement.

The implementation uses SparkSQL as it seemed the natural way to go for joins. This part is easy to develop for someone familiar with relational databases and SQL concepts
Another positive aspect is that the code can be tested "locally", in standalone mode before executing it in the cluster. The Hive details are largely hidden from the 
programmer. In this case it wouldn't make a huge difference, but getting feedback within the development environment allows for quick implementation of working solutions.


## HBase

The HBase part was more involved, when compared to the other ones. It was not entirely clear whether the solution had to use Spark or just the "native" HBase API
in scala so I did a bit of both. Uploading the data was done with Spark and bulkPut (which is probably a better choice for large amounts of data) instead of a list of Puts.
The update and scan part were implemented directly using the HBase API, which seems easier than working with RDDs for this particular problem. 

I had some trouble determining row keys, started using integers in sequence but this turns out to be a bad practice (again, my limited understanding of the HBase fundamentals
doesn't help) so it was not clear to me how I could append items from the second source while avoiding duplicate id's. In the end I assumed that the combination 
`driverId-eventId` would be unique and I used that as a row key. Therefore updating the fourth item required a bit different logic, but the formulation of the problem 
was a bit confusing anyway (I wouldn't assume that the fourth entry contains a specific route). Scanning seems to be a powerful mechanism for retrieving table data,
but processing the results seems more complicated than needed, at least to the untrained eye. That said, I had never worked with a column-based database before so this
was an enlightening experience.

