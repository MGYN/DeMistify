import csv
import os
import sys

import community
import matplotlib.pyplot as plt
import networkx as nx


class graph:
    def __init__(self, path):
        self.G = nx.Graph()
        self.edge_list = []
        self.result = {}
        self.path = path
        self.nativeClass = ""
        self.first = 0

    def getEdge(self, result=None, index=None, weight=True):
        if result:
            self.result = {}
            self.edge_list = []
            self.G = nx.Graph()
        with open(self.path, 'r') as f:
            for line in f:
                flag = True
                row = line.strip().split("\t")
                cols = row[0].strip().split("--")
                y1 = cols[0]
                y2 = cols[1]
                if result:
                    total = []
                    for i in index:
                        for j in result[i]:
                            total.append(j)
                    if not (y1 in total and y2 in total):
                        continue
                if weight:
                    edge = (y1, y2, float(row[1]))  # 元组代表一条边
                else:
                    edge = (y1, y2)  # 元组代表一条边
                self.edge_list.append(edge)

    def getPartition(self, resolution,first=True):
        # G.add_nodes_from(nodeset)
        self.G.add_weighted_edges_from(self.edge_list)
        nx.transitivity(self.G)
        self.part = community.best_partition(self.G, resolution=resolution)
        if first:
            part_length = set()
            for i in self.part:
                part_length.add(self.part.get(i))
            self.first = len(part_length)

    def collectNode(self):
        for node in self.G.nodes():
            if self.result.get(self.part.get(node)) == None:
                self.result[self.part.get(node)] = []
            self.result[self.part.get(node)].append(node)

    def printResult(self):
        for i in self.result:
            print("******************************")
            for j in self.result[i]:
                print(j)

    def writeFirstResult(self):
        write = open(self.path.replace("edge", "first_community"), 'w', encoding='UTF-8')
        for i in self.result:
            for j in self.result[i]:
                write.write(j + "\n")
            write.write("******************************\n")
        write.close()
        print("%s&%s"%(self.first,self.path.replace("edge", "first_community")))

    def writeResult(self):
        border = []
        for i in self.result:
            for j in self.result[i]:
                border.append(j)
            for native in self.nativeClass:
                if native in border:
                    break
            border.clear()
        write = open(self.path.replace("edge", "community"), 'w', encoding='UTF-8')
        for cls in border:
            write.write(cls + "\n")
        write.close()
        print(self.path.replace("edge", "community"))

    def write_csv(self, path, apk_name):
        node_list = []
        egde_csv = open(os.path.join(path, apk_name + '_egde.csv'), 'w', encoding='utf-8', newline="")
        csv_writer_edge = csv.writer(egde_csv)
        csv_writer_edge.writerow(["Source", "Target", "Weight", "Type"])
        node_csv = open(os.path.join(path, apk_name + '_node.csv'), 'w', encoding='utf-8', newline="")
        csv_writer_node = csv.writer(node_csv)
        csv_writer_node.writerow(["Id", "Label"])
        with open(self.path, 'r') as f:
            for line in f:
                row = line.strip().split("\t")
                cols = row[0].strip().split("--")
                y1 = cols[0]
                y2 = cols[1]
                if y1 not in node_list:
                    csv_writer_node.writerow([y1, y1])
                    node_list.append(y1)
                if y2 not in node_list:
                    csv_writer_node.writerow([y2, y2])
                    node_list.append(y2)
                csv_writer_edge.writerow([y1, y2, row[1], "directed"])

    def twoTimes(self, nativeClass, resolution1, resolution2):
        self.nativeClass = nativeClass
        self.getEdge()
        self.getPartition(resolution1)
        self.collectNode()
        index = set()
        for native in nativeClass:
            index.add(self.part.get(native))
        self.getEdge(result=self.result, index=index)
        self.getPartition(resolution2,False)
        self.collectNode()

    def oneTimes(self, resolution):
        self.getEdge()
        self.getPartition(resolution)
        self.collectNode()
path = sys.argv[1]
native = sys.argv[2]
result = native.split(',')
nativeClass = []
for r in result:
    nativeClass.append(r.strip('[').strip(']').strip(' '))
g = graph(path)
g.twoTimes(nativeClass, 2.0, 0.5)
g.writeFirstResult()
# g.writeResult()
# g.printResult()
# g.write_csv(r"C:\Users\RPC_7\Desktop\result", "com.hypr.one")
