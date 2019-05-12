
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
        return np.arange(0, 4501, 900)
    if index == 2:
        return np.arange(0, 3001, 750)
    if index == 3:
        return np.arange(0, 1001, 250)

def tps(dirs, oPath):
    rows = 1
    cols = 3
    index = 1

    n = 0
    names=['$\\beta=10$', '$\\beta=100$', '$\\beta=1000$']
    txSize = [512, 1024, 4096]
    blockSize = [10, 100, 1000]
    fig, ax = plt.subplots(nrows=rows, ncols=cols, figsize=(8, 2.5))
    plt.subplots_adjust(wspace=0.2, hspace=0.5)

    for d in dirs:
        files = glob.glob(d + "/summeries/sigs/*.csv")
        df = csvs2df(files)

        for bs in blockSize:
            sb = str(rows) + str(cols) + str(index)
            sb = int(sb)
            plt.subplot(sb)
            m = 0
            for ts in txSize:
                data = df[(df.txSize == bs) & (df.blockSize == ts)]
                # data = data[['workers', 'sps']]

                mark = markers[m]
                m += 1
                data = data[['workers', 'sps']].groupby(data.workers).mean()
                plt.plot(data['workers'], data['sps'], "-" + mark, markerfacecolor=face_c,
                             markersize=marker_s, linewidth=line_w) #, markevery=markers_on)

            plt.title(names[n], fontsize=fs)
            plt.xticks(np.arange(0, 11, step=2), fontsize=fs)
            plt.yticks(getYrange(index), fontsize=fs)
            plt.grid(True)
            n += 1
            index += 1

    leg = fig.legend([],  # The line objects
                     labels=['$\\sigma=512$', '$\\sigma=1K$', '$\\sigma=4K$'],
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
    fig.text(0.021, 0.5, "SPS ($\\frac{signatures}{sec}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.03, 0.1, 1, 1.03])
    for d in oPath:
        plt.savefig(d + '/sigs.pdf')
        plt.savefig(d + '/sigs')

if __name__ == "__main__":
    tps(["/home/yoni/toy/m5/sigs"
         ],
        ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/figures"])