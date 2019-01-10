
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
        return np.arange(0, 200, 50)
    if index == 3:
        return np.arange(0, 500, 100)
    if index == 4:
        return np.arange(0, 16, 5)
    if index == 5:
        return np.arange(0, 101, 20)
    if index == 6:
        return np.arange(0, 301, 100)
    if index == 7:
        return np.arange(0, 11, 2)
    if index == 8:
        return np.arange(0, 81, 20)
    if index == 9:
        return np.arange(0, 301, 100)
def plotTxthroughput(dirs, oPath):
    rows = 3
    cols = 3
    index = 1
    evSize = ['10', '100', '1000']
    bSize = ['50', '512', '1024', '4096']
    fSize = ['0', '500', '1012', '4084']
    nu="$n=$"
    beta="$\\beta=$"
    names = [nu+'4, ' + beta + '10',nu+'4, ' + beta + '100'
              , nu+'4, ' + beta + '1000', nu+'7, ' + beta + '10'
              , nu+'7, ' + beta + '100', nu+'7, ' + beta + '1000'
              , nu+'10, ' + beta + '10', nu+'10, ' + beta + '100'
              , nu+'10, ' + beta + '1000']
    n = 0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.3, hspace=0.6)
    r, c = 0, 0
    lines = []
    for d in dirs:
        c = 0
        for size in evSize:
            sb = str(rows) + str(cols) + str(index)
            sb = int(sb)
            plt.subplot(sb)
            m = 0
            for f in fSize:
                mark = markers[m]
                m+= 1
                files = glob.glob(d + "/*." + f + "." + size + "/servers/res/summery.csv")
                df = csvs2df(files)
                df = df[['channels', 'txPsec']][(df.channels == 1) | (df.channels % 2 == 0)].groupby(df.channels).mean()
                markers_on=[0, 3, 6, 10]
                l = plt.plot(df['channels'], df['txPsec'] / 1000, "-" + mark, markerfacecolor=face_c, markersize=marker_s, linewidth=line_w, markevery=markers_on)

            plt.title(names[n], fontsize='large')
            plt.xticks(np.arange(0, 21, step=5), fontsize=fs)
            plt.yticks(getYrange(index), fontsize=fs)
            c += 1
            plt.grid(True)
            n += 1
            index += 1
        r += 1
    leg = fig.legend(lines,  # The line objects
               labels=['$\\sigma=50B$', '$\\sigma=512B$', '$\\sigma=1KB$', '$\\sigma=4KB$'],  # The labels for each line
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
    fig.text(0.025, 0.5, "Throughput ($\\frac{KTx}{sec}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.02, 0.06, 1, 1.03])
    for d in oPath:
        plt.savefig(d + '/throughput2.pdf')
        plt.savefig(d + '/throughput2')

if __name__ == "__main__":
    plotTxthroughput(["/home/yoni/toy/singleDCThroughput/4Servers", "/home/yoni/toy/singleDCThroughput/7Servers",
                      "/home/yoni/toy/singleDCThroughput/10Servers"],
                         ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])