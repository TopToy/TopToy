from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']


def plotGdLatencyChart(dirs, oPath):
    n_groups = 3
    bar_width = 0.35
    opacity = 1
    subDirs = ["clients/500.100", "clients/500.1000"]
    names = ['100 Txs/block', '1000 Txs/block']
    rows = 1
    cols = 2
    index = 1
    n=0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.3, hspace=0.5)
    lines = []
    for d in subDirs:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        ax2 = plt.subplot(sb)
        index2 = np.arange(n_groups)
        mean = [[], []]
        stdUp = [[], []]
        stdLow = [[], []]
        stds = [[], []]
        medians = [[], []]
        data = []
        for dir in dirs:

            files = ["blocksStat_1.csv", "blocksStat_5.csv"]
            for i in range(0, 2):
                f = files[i]
                path = dir + "/" + d + "/" + f
                df = pd.read_csv(path, sep=",")
                len = df.size

                df = df['clientLatency'] / 1000
                data += [df]
                print("path: " + path)
                print("max: " + str(df.max()))
                print("min: " + str(df.min()))
                print("size:" + str(len))
        plt.boxplot(data, 0, '', whis=[0, 90])
        ax2.set_xticklabels(('4(1)', '4(5)', '7(1)', '7(5)', '10(1)', '10(5)'))
        plt.title(names[index - 1], fontsize='medium')
        plt.grid(True)
        index += 1

    fig.text(0.48, 0.03, "Servers(workers)", ha="center", va="center")
    fig.text(0.03, 0.5, "Time (seconds)", ha="center", va="center", rotation=90)
    fig.tight_layout(rect=[0.01, 0.03, 0.93, 1])
    for d in oPath:
        plt.savefig(d + '/gd_cdf3.pdf', bbox_inches='tight')
        plt.savefig(d + '/gd_cdf3', bbox_inches='tight')

if __name__ == "__main__":
    plotGdLatencyChart(["/home/yoni/toy/old/gd_latency/4Servers"
                       , "/home/yoni/toy/old/gd_latency/7Servers"
                       , "/home/yoni/toy/old/gd_latency/10Servers"]
                   , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])