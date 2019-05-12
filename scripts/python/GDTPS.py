
from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df
fs=18

line_w=1
marker_s=5
face_c='none'
markers=['s', 'x', '+', '^']

def getYrange(index):
    if index == 1:
        return np.arange(0, 16001, 4000)
    if index == 2:
        return np.arange(0, 25001, 5000)
    if index == 3:
        return np.arange(0, 32001, 6000)
    if index == 4:
        return np.arange(0, 21, 5)
    if index == 5:
        return np.arange(0, 130, 30)
    if index == 6:
        return np.arange(0, 210, 40)
    if index == 7:
        return np.arange(0, 21, 5)
    if index == 8:
        return np.arange(0, 130, 30)
    if index == 9:
        return np.arange(0, 210, 40)

def tps(dirs, oPath):
    rows = 1
    cols = 3
    index = 1

    nu="$n=$"
    # beta="$\\beta=$"
    beta = "$\\beta=$"
    names = [nu + '4', nu + '7', nu + '10']
    n = 0
    txSize = [512]
    blockSize = [10, 100, 1000]
    fig, ax = plt.subplots(nrows=rows, ncols=cols, figsize=(8, 2.5))
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    r, c = 0, 0
    lines = []
    # markers_on = [0, 1, 3, 5]
    for d in dirs:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        plt.subplot(sb)
        files = glob.glob(d + "/summeries/*.csv")
        df = csvs2df(files)
        m = 0
        for bs in blockSize:
            data = df[(df.txSize == 512) & (df.txInBlock == bs)]
            data = data[['workers', 'tps']]


            mark = markers[m]
            m += 1
            data = data[['workers', 'tps']].groupby(data.workers).mean()
            plt.plot(data['workers'], data['tps'], "-" + mark, markerfacecolor=face_c,
                             markersize=marker_s, linewidth=line_w) #, markevery=markers_on)

        plt.title(names[n], fontsize=fs)
        plt.xticks(np.arange(0, 11, step=2), fontsize=fs)
        plt.yticks(getYrange(index), fontsize=fs)
        plt.grid(True)
        n += 1
        index += 1

    leg = fig.legend(lines,  # The line objects
                     labels=['$\\beta=10$', '$\\beta=100$', '$\\beta=1000$'],
                     # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=fs,
                     ncol=3,
                     frameon=False,
                     bbox_to_anchor=(0.5, -0.1),
                     #  title = "Tx size\n(Bytes)"
                     )
    # plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.51, 0.12, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.019, 0.5, "TPS ($\\frac{transactions}{sec}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.02, 0.1, 1, 1.03])
    for d in oPath:
        plt.savefig(d + '/gdtps2.pdf')
        plt.savefig(d + '/gdtps2')

if __name__ == "__main__":
    tps(["/home/yoni/toy/m5/gd/correct/4",
         "/home/yoni/toy/m5/gd/correct/7",
         "/home/yoni/toy/m5/gd/correct/10"
         ],
        ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/figures"])