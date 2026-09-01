const int BOMB_DATA_SIZE = 10;

layout(std140) uniform InstanceData {
    int count;
    float data[20];
};
