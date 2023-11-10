import leidenalg
import networkx as nx
from igraph import *

import community


# path = r'C:\Users\RPC_7\Desktop\total\com.znit.face\com.znit.face_edge.txt'
# native = "com.seeta.sdk.FaceEyeStateDetector"

# path = r"C:\Users\RPC_7\Desktop\total\com.hefe.pro.editor\com.hefe.pro.editor_edge.txt"
# nativeClass = ["com.lofi.photo.edit.facecheck.tracker.MLTrackerEngine"]


class cluster_graph:
    def __init__(self):
        self.edge_list = []
        self.weight = []
        self.nodes = []
        self.result = []

    def collect_info(self, path):
        self.path = path
        with open(path, 'r') as f:
            for line in f:
                row = line.strip().split("\t")
                cols = row[0].strip().split("--")
                y1 = cols[0]
                y2 = cols[1]
                edge = (y1, y2)  # 元组代表一条边
                if y1 not in self.nodes:
                    self.nodes.append(y1)
                if y2 not in self.nodes:
                    self.nodes.append(y2)
                self.weight.append(float(row[1]))
                self.edge_list.append(edge)

    def cluster(self, nativeClass, times=2):
        G = Graph.TupleList(self.edge_list, directed=False, weights=True)
        part = leidenalg.find_partition(G, leidenalg.ModularityVertexPartition, weights=self.weight, n_iterations=times)
        for i in part:
            for j in i:
                if self.nodes[j] in nativeClass:
                    for k in i:
                        self.result.append(self.nodes[k])
                    break

    def get_result(self):
        for i in self.result:
            print(i)

    def writeResult(self):
        write = open(self.path.replace("edge", "first_community"), 'w', encoding='UTF-8')
        for i in self.result:
            write.write(i + "\n")
        write.write("******************************\n")
        write.close()
        print(self.path.replace("edge", "first_community"))


path = sys.argv[1]
native = sys.argv[2]
result = native.split(',')
nativeClass = []
for r in result:
    nativeClass.append(r.strip('[').strip(']').strip(' '))
g = cluster_graph()
g.collect_info(path)
g.cluster(nativeClass)
g.writeResult()
