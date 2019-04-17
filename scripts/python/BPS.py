
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

def bps(dirs, oPath):
    rows = 2
    cols = 3
    index = 1
    # evSize = ['10', '100', '1000']
    # bSize = ['50', '512', '1024', '4096']
    # fSize = ['0', '500', '1012', '4084']
    nu="$n=$"
    # beta="$\\beta=$"
    names = [nu+'4', nu+'7', nu+'10']
    n = 0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    # plt.subplots_adjust(wspace=0.3, hspace=0.6)
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    r, c = 0, 0
    lines = []
    tmos=[1, 10, 100]
    markers_on = [0, 3, 6, 9]
    for d in dirs:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        plt.subplot(sb)
        files = glob.glob(d + "/**/servers/res/summery.csv")
        df = csvs2df(files)
        df = df[['workers', 'bps', 'tmo']]
        m = 0
        for tmo in tmos:
            mark = markers[m]
            m += 1
            data=df[(df.tmo == tmo)].groupby(df.workers).mean()
            plt.plot(data['workers'], data['bps'], "-" + mark, markerfacecolor=face_c, markersize=marker_s,
                     linewidth=line_w, markevery=markers_on)
        plt.title(names[n], fontsize='large')
        plt.xticks(np.arange(0, 11, step=2), fontsize=fs)
        plt.yticks(np.arange(450, 1100, step=300), fontsize=fs)
        plt.grid(True)
        n += 1
        index += 1



    leg = fig.legend(lines,  # The line objects
               labels=['$\\tau=10\\lambda$', '$\\tau=100\\lambda$', '$\\tau=1000\\lambda$', '$\\tau=10000\\lambda$'],  # The labels for each line
               loc="lower center",  # Position of legend
               # borderaxespad=0.01,  # Small spacing around legend box
               fontsize=fs,
                ncol=4,
               frameon=False,
               bbox_to_anchor=(0.5, -0.03),
               #  title = "Tx size\n(Bytes)"
               )
    # plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.51, 0.06, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.025, 0.5, "KBPS ($\\frac{KBlocks}{sec}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.02, 0.06, 1, 1.03])
    for d in oPath:
        plt.savefig(d + '/bps.pdf')
        plt.savefig(d + '/bps')

if __name__ == "__main__":
    bps(["/home/yoni/toy/res"],
                         ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])