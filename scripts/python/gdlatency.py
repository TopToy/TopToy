from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from utiles import csvs2df

fs=14

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

def getYlabels(index) :
    if index == 1:
        return np.arange(0, 7, 2)
    if index == 2:
        return np.arange(0, 9, 2)
    if index == 3:
        return np.arange(0, 36, 7)
    if index == 4:
        return np.arange(0, 9, 2)
    if index ==5:
        return np.arange(0, 13, 3)
    if index == 6:
        return np.arange(0, 26, 5)
    if index == 7:
        return np.arange(0, 13, 3)
    if index == 8:
        return np.arange(0, 17, 4)
    if index == 9:
        return np.arange(0, 36, 7)

def plotGdLatencyChart(dirs, oPath):
    n_groups = 3
    bar_width = 0.35
    opacity = 1
    names = ['$n=4, \\beta=10$', '$n=4, \\beta=100$', '$n=4, \\beta=1000$'
             , '$n=7, \\beta=10$', '$n=7, \\beta=100$', '$n=7, \\beta=1000$'
             , '$n=10, \\beta=10$', '$n=10, \\beta=100$', '$n=10, \\beta=1000$'
             ]
    beta=[10, 100, 1000]
    workers=[1, 5, 10]
    rows = 3
    cols = 3
    index = 1
    n=0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)#,  figsize=(4.5, 3.5))
    plt.subplots_adjust(wspace=0.3, hspace=0.5)
    lines = []
    for d in dirs:
        files = glob.glob(d + "/summeries/blocks/*.csv")
        df = csvs2df(files)
        for b in beta:
            data = []
            sb = str(rows) + str(cols) + str(index)
            sb = int(sb)
            ax2 = plt.subplot(sb)
            bdata = df[(df.maxTxInBlock == b)]
            for w in workers:
                wdata = bdata[(bdata.workers == w)]
                data += [wdata['TimeToDeliver'] / 1000]
                print("path: " + d)
                print("max: " + str(bdata.max()))
                print("min: " + str(bdata.min()))
                print("size:" + str(len(bdata)))
            plt.boxplot(data, 0, '', whis=[5, 95])
            ax2.set_xticklabels(('1', '5', '10'),fontsize=fs)
            ax2.set_yticklabels(getYlabels(index), fontsize=fs)
            plt.yticks(getYlabels(index), fontsize=fs)
            plt.title(names[index - 1], fontsize=fs)
            plt.grid(True)
            index += 1

    fig.text(0.503, 0.03, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.03, 0.5, "Time (seconds)", ha="center", va="center", rotation=90, fontsize=fs)
    fig.tight_layout(rect=[0.01, 0.02, 1, 1])
    for d in oPath:
        plt.savefig(d + '/gd_latency.pdf', bbox_inches='tight')
        plt.savefig(d + '/gd_latency', bbox_inches='tight')

if __name__ == "__main__":
    plotGdLatencyChart([
                        "/home/yoni/toy/m5/gd/correct/4"
                        ,
                        "/home/yoni/toy/m5/gd/correct/7"
                        ,
                        "/home/yoni/toy/m5/gd/correct/10"
                        ]
                   , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/figures"])