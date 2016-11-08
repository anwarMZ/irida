import Phylocanvas from 'phylocanvas';
import metadataPlugin from 'phylocanvas-plugin-metadata';
import {METADATA} from './../../constants';

const PHYLOCANVAS_DIV = 'phylocanvas';

Phylocanvas.plugin(metadataPlugin);

const metadataFormat = {
  showHeaders: true,
  showLabels: true,
  blockLength: 32,
  blockSize: 32,
  padding: 18,
  columns: [],
  propertyName: 'data',
  underlineHeaders: true,
  headerAngle: 0,
  fillStyle: 'black',
  strokeStyle: 'black',
  lineWidth: 1,
  font: null
};

const setCanvasHeight = $window => {
  const canvas = document.querySelector(`#${PHYLOCANVAS_DIV}`);
  canvas.style.height = `${$window.innerHeight - 200}px`;
};

/**
 * Angular controller function for this scope.
 * @param {object} $window AngularJS window object
 * @param {object} $scope AngularJS $scope object for current dom
 * @param {object} PhylocanvasService angular service for server exchanges for newick data
 */
function controller($window, $scope, PhylocanvasService) {
  // Make the canvas fill the viewable window.
  setCanvasHeight($window);

  // Initialize phylocanvas.
  const tree = Phylocanvas
    .createTree(PHYLOCANVAS_DIV, {
      metadata: metadataFormat
    });

  /**
   * Update the tree leaves with new metadata
   */
  const updateMetadata = () => {
    let prev;
    tree.leaves.forEach(leaf => {
      const data = this.metadata[leaf.label];
      if (data) {
        leaf.data = data;
      } else {
        leaf.data = prev;
      }
      prev = Object.assign({}, data);
    });
    if (tree.drawn) {
      tree.draw();
    }
  };

  // Set tree defaults
  tree.setTreeType('rectangular');
  tree.alignLabels = true;
  // tree.on('beforeFirstDraw', () => updateMetadata());

  /**
   * Listen for changes to the metadata structure and update
   * the phylocanvas accordingly.
   */
  $scope.$on(METADATA.UPDATED, (event, args) => {
    this.metadata = args.metadata;
    if (tree.drawn) {
      updateMetadata();
    } else {
      // Load the tree only when the initial metadata is available.
      tree.load(this.newick);
    }
  });

  /**
   * Kick everything off by getting the newick file and the
   * initial metadata.
   */
  PhylocanvasService.getNewickData(this.newickurl)
    .then(data => {
      this.newick = data;
      tree.load(this.newick);
    });
}

export const PhylocanvasComponent = {
  bindings: {
    newickurl: '@',
    template: '@'
  },
  templateUrl: 'phylocanvas.tmpl.html',
  controller
};
