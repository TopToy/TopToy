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

def getTmoYrange(index):
    if index == 1:
        return np.arange(0.9, 1.01, 0.05)
    if index == 2:
        return np.arange(0.4, 1.01, 0.2)
    if index == 3:
        return np.arange(0.8, 1.01, 0.1)
    if index == 4:
        return np.arange(0.2, 1.01, 0.2)
    if index == 5:
        return np.arange(0.8, 1.01, 0.1)
    if index == 6:
        return np.arange(0, 1.01, 0.2)

def plotTMO(dirs, oPath):
    rows=3
    cols=2
    index=1
    series=[5,10,15,20]
    names=["$n=4, \\beta=100$", "$n=4, \\beta=1000$",
           "$n=7, \\beta=100$", "$n=7, \\beta=1000$"
           ,"$n=10, \\beta=100$", "$n=10, \\beta=1000$"]
    n=0

    allDirs = [dirs[0] + "/100.500", dirs[0] + "/1000.500", dirs[1] + "/100.500", dirs[1] + "/1000.500"
               , dirs[2] + "/100.500", dirs[2] + "/1000.500"]
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.3, hspace=0.5)
    for d in allDirs:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        plt.subplot(sb)
        m = 0
        for s in series:
            mark = markers[m]
            m += 1
            path=d + "/summery_" + str(s) + ".csv"
            df = pd.read_csv(path, sep=",")
            df = df[['tmo', 'opRate']].groupby(df.tmo).mean()
            markers_on = [0, 3, 7, 9]
            plt.plot(df['tmo'], df['opRate'],"-" + mark, markerfacecolor=face_c, markersize=marker_s, linewidth=line_w, markevery=markers_on)

        plt.title(names[n], fontsize=fs)
        plt.grid(True)
        plt.xticks(np.arange(0, 1001, step=200), fontsize=fs)
        plt.yticks(getTmoYrange(index), fontsize=fs)
        n += 1
        index += 1
    leg = fig.legend([],  # The line objects
               labels=['$\\omega=5$', '$\\omega=10$', '$\\omega=15$', '$\\omega=20$'],  # The labels for each line
               loc="lower center",  # Position of legend
               # borderaxespad=0.01,  # Small spacing around legend box
               fontsize=fs,
               frameon=False,
                ncol=4,
               bbox_to_anchor=(0.5, -0.02),
               # title="Channels"
               # title_fontsize="x-small"
               )
    plt.setp(leg.get_title(), fontsize=fs)
    fig.text(0.51, 0.06, "$\\tau$", ha="center", va="center", fontsize=fs)
    fig.text(0.03, 0.5, "Optimistic rate ($\\frac{op_b}{total_b}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.01, 0.05, 1, 1.03])
    for d in oPath:
        plt.savefig(d + '/tmo.pdf')
        plt.savefig(d + '/tmo')

if __name__ == '__main__':
    plotTMO(["/home/yoni/toy/old/tmo/4Servers", "/home/yoni/toy/old/tmo/7Servers",
                     "/home/yoni/toy/old/tmo/10Servers"], ["/home/yoni/toy/figures",
                                                       "/home/yoni/Dropbox/paper/draws"])