A simple World Edit extension for creating long, curved (or not) holes. Basically a long brush++

## Usage
Using the world edit wand, select the line which you want to burrow through. Next, use the command:

### ```//mole <radius> [precision] [cube] [state]```
| Argument  | Possible Values |  Description                                                                                                | Default Value |
|:---------:|-----------------|:------------------------------------------------------------------------------------------------------------|:-------------:|
|  radius   |     double      | *Radius of the sphere **or** dimensions of the cube burrow. **(Required)***                                 |       -       |
| precision |     double      | *Distance between checks. Higher value means more inconsistencies,<br/>but is faster for larger operations* |      0.1      |
|   cube    |   true, false   | *Whether to burrow using a cube mask*                                                                       |     false     |
|   state   |    material     | *The block type to replace the burrowed area*                                                               |      AIR      |
|   state   | block position  | *The specific block with which to replace the burrowed area*                                                |      AIR      |

## Curves
This mod uses the <a href="https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Quadratic_B%C3%A9zier_curves">*Quadratic Bézier curve*</a> for its curvature.
You can select the control point with ```//ctrl [pos]``` to curve it in that direction, and do ```//ctrl unset``` to remove the control point