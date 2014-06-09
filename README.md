spring-batch-remote-partition-test
==================================
Demonstration for this issue

http://stackoverflow.com/questions/24103657/can-we-run-multiple-job-instances-of-same-job-with-different-parameters-at-the-s

This is spring batch admin application.

Dummy input data https://github.com/vishalmelmatti/spring-batch-remote-partition-test/tree/master/src/main/resources/sample-data

2014-06-08/exchanges.txt, below data should go to log file /home/[USER]/tmp/spring/batch/batch.log.2014-06-08

1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30

2014-06-09/exchanges.txt, below data should go to log file /home/[USER]/tmp/spring/batch/batch.log.2014-06-09
31
32
33
34
35
36
37
38
39
40
41
42
43
44
45
46
47
48
49
50
51
52
53
54
55
56
57
58
59
60


Job  and partitioning configurations

https://github.com/vishalmelmatti/spring-batch-remote-partition-test/blob/master/src/main/resources/META-INF/spring/batch/jobs/import-exchanges.xml

Item writer
https://github.com/vishalmelmatti/spring-batch-remote-partition-test/blob/master/src/main/java/com/st/batch/foundation/ImportExchangesItemWriter.java

Command Runner

https://github.com/vishalmelmatti/spring-batch-remote-partition-test/tree/master/src/main/java/com/st/symfony

To reproduce the issue, jobs should be launched with input parameters

batch_id=2014-06-08 and
batch_id=2014-06-09

But if we launch these two jobs simuleteneously, data gets mixed, this is output I am getting

/home/[USER]/tmp/spring/batch/batch.log.2014-06-08

23
19
27
1
14
9
24
20
28
2
15
10
21
25
29
3
16
11
22
26
30
17
4
12


/home/[USER]/tmp/spring/batch/batch.log.2014-06-09

18
13
5
57
47
31
6
58
48
32
52
42
7
59
49
33
53
43
8
60
50
34
54
44
37
51
35
55
45
38
36
56
46
39
40
41

