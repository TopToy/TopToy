
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
        return np.arange(0, 21, 5)
    if index == 2:
        return np.arange(0, 130, 30)
    if index == 3:
        return np.arange(0, 210, 40)
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
    rows = 3
    cols = 3
    index = 1

    nu="$n=$"
    # beta="$\\beta=$"
    beta = "$\\beta=$"
    names = [nu + '4, ' + beta + '10', nu + '4, ' + beta + '100'
        , nu + '4, ' + beta + '1000', nu + '7, ' + beta + '10'
        , nu + '7, ' + beta + '100', nu + '7, ' + beta + '1000'
        , nu + '10, ' + beta + '10', nu + '10, ' + beta + '100'
        , nu + '10, ' + beta + '1000']
    n = 0
    txSize = [512, 1024, 4096]
    blockSize = [10, 100, 1000]
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    r, c = 0, 0
    lines = []
    # markers_on = [0, 1, 3, 5]
    for d in dirs:
        for bs in blockSize:
            sb = str(rows) + str(cols) + str(index)
            sb = int(sb)
            plt.subplot(sb)
            files = glob.glob(d + "/summeries/*.csv")
            df = csvs2df(files)
            for ts in txSize:
                data = df[(df.txSize == ts) & (df.txInBlock == bs)]
                data = data[['workers', 'tps']]
                m = 0

                mark = markers[m]
                m += 1
                data = data[['workers', 'tps']].groupby(df.workers).mean()
                plt.plot(data['workers'], data['tps'] / 1000, "-" + mark, markerfacecolor=face_c,
                             markersize=marker_s, linewidth=line_w) #, markevery=markers_on)
            plt.title(names[n], fontsize='large')
            plt.xticks(np.arange(0, 11, step=2), fontsize=fs)
            plt.yticks(getYrange(index), fontsize=fs)
            plt.grid(True)
            n += 1
            index += 1

    leg = fig.legend(lines,  # The line objects
                     labels=['$\\sigma=512B$', '$\\sigma=1KB$', '$\\sigma=4KB$'],
                     # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=fs,
                     ncol=3,
                     frameon=False,
                     bbox_to_anchor=(0.5, -0.03),
                     #  title = "Tx size\n(Bytes)"
                     )
    # plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.51, 0.06, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.025, 0.5, "TPS ($\\frac{transactions}{sec}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.02, 0.04, 1, 1.03])
    for d in oPath:
        plt.savefig(d + '/tps.pdf')
        plt.savefig(d + '/tps')

if __name__ == "__main__":
    tps(["/home/yoni/toy/correct/4",
         "/home/yoni/toy/correct/7",
         "/home/yoni/toy/correct/10"
         ],
        ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])