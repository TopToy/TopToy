from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df

fs=14
line_w=1
marker_s=5
face_c='none'
markers=['s', 'x', '+', '^']

def calcCDFX(index):
    if index == 1:
        return np.arange(0, 2001, 500)
    if index == 2:
        return np.arange(0, 3001, 1000)
    if index == 3:
        return np.arange(0, 6001, 2000)
    if index == 4:
        return np.arange(0, 2001, 500)
    if index == 5:
        return np.arange(0, 3001, 1000)
    if index == 6:
        return np.arange(0, 8001, 2000)
    if index == 7:
        return np.arange(0, 2501, 500)
    if index == 8:
        return np.arange(0, 5001, 1000)
    if index == 9:
        return np.arange(0, 8001, 2000)

def latency(dirs, oPath):

    names = ['$n=4, \\beta=10$', '$n=4, \\beta=100$', '$n=4, \\beta=1000$',
             '$n=7, \\beta=10$', '$n=7, \\beta=100$','$n=7, \\beta=1000$',
             '$n=10, \\beta=10$', '$n=10, \\beta=100$', '$n=10, \\beta=1000$']
    rows = 3
    cols = 3
    index = 1
    n=0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.3, hspace=0.5)
    beta=[10, 100, 1000]
    workers=[1, 5, 10]
    lines = []
    for dir in dirs:
        m=0
        files = glob.glob(dir + "/summeries/blocks/*.csv")
        df = csvs2df(files)
        for b in beta:
            sb = str(rows) + str(cols) + str(index)
            sb = int(sb)
            plt.subplot(sb)
            data=df[(df.maxTxInBlock == b)]
            for w in workers:
                mark = markers[m]
                wdata=data[data.workers == w]
                wdata=wdata['TimeToDeliver']
                print("[w= " + str(w) + " ; beta= " + str(b) + " ; input=" + dir + "]")
                print("max: " + str(wdata.max()))
                print("min: " + str(wdata.min()))
                print("size:" + str(wdata.size))
                num_bins = 100
                counts, bin_edges = np.histogram(wdata / 1000, bins=num_bins)#, normed=True)
                cdf = np.cumsum(counts)
                markers_on = [0, 33, 66, 99]
                plt.plot(bin_edges[1:], cdf / cdf[-1], "-" + mark, markerfacecolor=face_c,
                         markersize=6, linewidth=line_w, markevery=markers_on)
            plt.title(names[n], fontsize=fs)
            plt.xticks(calcCDFX(index) / 1000, fontsize=fs)
            plt.yticks(np.arange(0, 1.01, 0.2), fontsize=fs)
            plt.grid(True)
            n += 1
            index += 1

    leg = fig.legend(lines,  # The line objects
                     labels=['$\\omega=1$', '$\\omega=5$', '$\\omega=10$'],  # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=fs,
                     frameon=False,
                     ncol=4,
                     bbox_to_anchor=(0.5, -0.02),
                     )
    plt.setp(leg.get_title(), fontsize=fs)
    # fig.text(0.5, 0.04, "Latency (seconds)", ha="center", va="center")
    # fig.text(0.05, 0.5, "percents of requests", ha="center", va="center", rotation=90)

    fig.text(0.51, 0.07, "Time (seconds)", ha="center", fontsize=fs, va="center")
    fig.text(0.02, 0.5, "Probability", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.02, 0.065, 1, 1.02])
    for d in oPath:
        plt.savefig(d + '/cdf_single_cloud.pdf')
        plt.savefig(d + '/cdf_single_cloud')

if __name__ == "__main__":
    latency([
            "/home/yoni/toy/m5/correct/4"
             ,"/home/yoni/toy/m5/correct/7"
             ,"/home/yoni/toy/m5/correct/10"
             ]
            , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/figures"])