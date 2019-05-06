
from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df
fs=10

line_w=1
marker_s=5
face_c='none'
markers=['s', 'x', '+', '^']

def bps(dirs, oPath):
    rows = 1
    cols = 1
    index = 1

    nu="$n=$"
    # beta="$\\beta=$"
    names = [nu+'4', nu+'7', nu+'10']
    n = 0
    fig, ax = plt.subplots(nrows=rows, ncols=cols, figsize=(3,2))
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    r, c = 0, 0
    lines = []
    markers_on = [0, 3, 6, 9]
    for d in dirs:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        plt.subplot(sb)
        files = glob.glob(d + "/summeries/*.csv")
        df = csvs2df(files)
        df = df[(df.txSize == 0) & (df.txInBlock == 0)]
        df = df[['workers', 'bps']]
        m = 0

        mark = markers[m]
        m += 1
        data = df[['workers', 'bps']].groupby(df.workers).mean()
        plt.plot(data['workers'], data['bps'], "-" + mark, markerfacecolor=face_c,
                 markersize=marker_s, linewidth=line_w) #, markevery=markers_on)
    plt.title("Blocks generation rate", fontsize=fs)
    plt.xticks(np.arange(0, 11, step=2), fontsize=fs)
    plt.yticks(np.arange(0, 2300, step=300), fontsize=fs)
    plt.grid(True)
    n += 1
    index += 1



    leg = fig.legend(lines,  # The line objects
               labels=['$n=4$', '$n=7$', '$n=10$'],  # The labels for each line
               loc="lower center",  # Position of legend
               # borderaxespad=0.01,  # Small spacing around legend box
               fontsize=10,
                ncol=3,
               frameon=False,
               bbox_to_anchor=(0.5, -0.07),
               #  title = "Tx size\n(Bytes)"
               )
    # plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.57, 0.1, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.045, 0.5, "BPS ($\\frac{blocks}{sec}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.04, 0.04, 1, 1.03])
    for d in oPath:
        plt.savefig(d + '/bps.pdf')
        plt.savefig(d + '/bps')

if __name__ == "__main__":
    bps(["/home/yoni/toy/correct/4",
         "/home/yoni/toy/correct/7",
         "/home/yoni/toy/correct/10"
         ],
        ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])