from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.patches import Rectangle

from utiles import csvs2df

fs=13.5

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

def plotNormal(dirs, oPath):

    xlab=['1','5','10']
    names=['$n=4, \\beta=10$', '$n=7, \\beta=10$', '$n=10, \\beta=10$'
           , '$n=4, \\beta=100$', '$n=7, \\beta=100$', '$n=10, \\beta=100$'
           , '$n=4, \\beta=1000$', '$n=7, \\beta=1000$', '$n=10, \\beta=1000$']
    labels = ['A-B', 'B-C', 'C-D', 'D-E']
    realLabels = ['BP2T','BP2D','BP2DL','HP2T','HP2D','HP2DL','HT2D','HD2DL']
    rows = 3
    cols = 3
    index = 1
    n = 0
    fig, ax = plt.subplots(nrows=rows, ncols=cols) #, figsize=(4, 4))
    plt.subplots_adjust(wspace=0.2, hspace=0)
    beta = [10, 100, 1000]
    workers = [1, 5, 10]

    for d in dirs:

        files = glob.glob(d + "/summeries/*.csv")
        df = csvs2df(files)
        m = 0
        for b in beta:
            sb = str(rows) + str(cols) + str(index)
            sb = int(sb)
            ax2 = plt.subplot(sb)
            bdata=df[df.txInBlock == b]
            ndata = []
            for w in workers:
                wdata=bdata[bdata.workers == w]
                ndata += [
                    [
                    (wdata['BP2T'].mean() - wdata['HP2T'].mean()) / wdata['BP2DL'].mean()
                    , wdata['HP2T'].mean() / wdata['BP2DL'].mean()
                    , wdata['HT2D'].mean()/ wdata['BP2DL'].mean()
                    , wdata['HD2DL'].mean()/ wdata['BP2DL'].mean()
                    ]
                ]
            im = ax2.imshow(ndata, cmap='Reds')
            ax2.set_xticks(np.arange(len(labels)))
            ax2.set_yticks(np.arange(len(xlab)))
            #
            ax2.set_yticklabels(xlab, fontsize=14)
            if index == 1 or index == 4 or index == 7:

                ax2.set_yticklabels(xlab, fontsize=16)
            else:
                plt.yticks([])
            #
            if index == 7 or index == 8 or index == 9:

                ax2.set_xticklabels(labels, fontsize=16)
            else:
                plt.xticks([])
            #
            #
            #
            ax2.set_xticks(np.arange(len(labels) + 1) - .5, minor=True)
            ax2.set_yticks(np.arange(len(xlab) + 1) - .5, minor=True)
            #
            for edge, spine in ax2.spines.items():
                spine.set_visible(False)
            #
            ax2.grid(which="minor", color="black", linestyle='-', linewidth=2)
            ax2.tick_params(which="minor", bottom=False, left=False)
            plt.setp(ax2.get_xticklabels(),
                     rotation_mode="anchor", fontsize=16)
            for i in range(len(xlab)):
                for j in range(len(labels)):
                    clr='black'
                    if round(ndata[i][j], 2) >= 0.3:
                        clr='white'
                    text = ax2.text(j, i, str(round(ndata[i][j], 2)),
                                   ha="center", va="center", color=clr, fontsize=fs)
            plt.title(names[index-1], fontsize=fs)
            index+=1
    fig.text(0.015, 0.76, "$\\omega$", ha="center", fontsize=fs, va="center", rotation=-90)
    fig.tight_layout(rect=[0, 0.2, 1, 1.3])
    for d in oPath:
        plt.savefig(d + '/heatmap2.pdf', bbox_inches='tight')
        plt.savefig(d + '/heatmap2', bbox_inches='tight')




def plotHM(dirs, oPath):
    plotNormal(dirs, oPath)

if __name__ == "__main__":
    plotHM([
                "/home/yoni/toy/m5/correct/4"
        ,
        "/home/yoni/toy/m5/correct/7"
        ,
        "/home/yoni/toy/m5/correct/10"
             ]
             , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/figures"])