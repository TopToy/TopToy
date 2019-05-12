
from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df
fs=12

line_w=1
marker_s=5
face_c='none'
markers=['s', 'x', '+', '^']

def getYrange(index):
    if index == 1:
        return np.arange(0, 66, 15)
    if index == 2:
        return np.arange(0, 41, 10)
    if index == 3:
        return np.arange(0, 61, 20)


def tps(dirs, oPath):
    rows = 1
    cols = 1
    index = 1

    nu="$n=$"
    # beta="$\\beta=$"
    beta = "$\\beta=$"
    names = ['$n=100$']
    n = 0
    txSize = [512]
    blockSize = [10, 100, 1000]
    fig, ax = plt.subplots(nrows=rows, ncols=cols, figsize=(3.5, 2))
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    r, c = 0, 0
    lines = []
    # markers_on = [0, 1, 3, 5]
    sb = str(rows) + str(cols) + str(index)
    sb = int(sb)
    plt.subplot(sb)
    for d in dirs:
        files = glob.glob(d + "/summeries/*.csv")
        df = csvs2df(files)
        m = 0
        for bs in blockSize:
            data = df[(df.txInBlock == bs) & (df.workers <= 5)]
            data = data[['workers', 'tps']]
            mark = markers[m]
            m += 1
            data = data[['workers', 'tps']].groupby(df.workers).mean()
            plt.plot(data['workers'], data['tps'] / 1000, "-" + mark, markerfacecolor=face_c,
                                 markersize=marker_s, linewidth=line_w) #, markevery=markers_on)
        plt.title(names[n], fontsize=fs)
        plt.xticks(np.arange(0, 6, step=1), fontsize=fs)
        plt.yticks(getYrange(index), fontsize=fs)
        plt.grid(True)
        n += 1
        index += 1

    leg = fig.legend(lines,  # The line objects
                     labels=['$\\beta=10$', '$\\beta=100$', '$\\beta=1000$'],
                     # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=12,
                     ncol=3,
                     frameon=False,
                     columnspacing=0.3,
                     bbox_to_anchor=(0.5, -0.095),
                     #  title = "Tx size\n(Bytes)"
                     )
    # plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.51, 0.12, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.05, 0.5, "KTPS ($\\frac{Ktransactions}{sec}$)", ha="center", va="center", fontsize=12, rotation=90)
    fig.tight_layout(rect=[0.06, 0.1, 1, 1.03])
    for d in oPath:
        plt.savefig(d + '/tps_bs.pdf')
        plt.savefig(d + '/tps_bs')

if __name__ == "__main__":
    tps(["/home/yoni/toy/m5/correct/100"],
         # ,"/home/yoni/toy/m5/byz1/7"
         # ,"/home/yoni/toy/m5/byz1/10"],
        ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/figures"])